package com.nikolasguillen.questlog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.account.DeleteAccountUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.GetSignInProviderUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.ObserveAccountUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.SignInUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.SignOutUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.BackUpUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.GetBackupInfoUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.HasLocalUserDataUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.RestoreBackupUseCase
import com.nikolasguillen.questlog.core.model.Account
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.settings.mapper.toUiModel
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupDialog
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEffect
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEvent
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiState
import com.nikolasguillen.questlog.feature.settings.model.BackupStatusUiModel
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_account_deleted
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_backup_complete
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_complete
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signed_in_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signed_out_message
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_BACKUP_LOADING_MILLIS = 400L

class AccountBackupViewModel(
    private val observeAccountUseCase: ObserveAccountUseCase,
    private val signInUseCase: SignInUseCase,
    getSignInProviderUseCase: GetSignInProviderUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val backUpUseCase: BackUpUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val hasLocalUserDataUseCase: HasLocalUserDataUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountBackupUiState(signInProvider = getSignInProviderUseCase()))
    internal val uiState: StateFlow<AccountBackupUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AccountBackupUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // The account whose backup status is on screen, so linking a second method (which emits the account again with
    // more providers) does not read the backup a second time.
    private var loadedForAccountId: String? = null

    init {
        observeAccount()
    }

    internal fun onEvent(event: AccountBackupUiEvent) {
        when (event) {
            AccountBackupUiEvent.SignIn -> signIn()
            AccountBackupUiEvent.BackUp -> backUp()
            AccountBackupUiEvent.ConfirmEmptyBackup -> {
                _uiState.update { it.copy(dialog = null) }
                launchOperation(CloudOperation.BACK_UP) { performBackUp() }
            }

            AccountBackupUiEvent.Restore -> restore()
            AccountBackupUiEvent.ConfirmRestore -> {
                _uiState.update { it.copy(dialog = null) }
                launchOperation(CloudOperation.RESTORE) { performRestore() }
            }

            AccountBackupUiEvent.SignOut -> {
                if (_uiState.value.runningOperation == null) {
                    _uiState.update { it.copy(dialog = AccountBackupDialog.ConfirmSignOut) }
                }
            }

            AccountBackupUiEvent.ConfirmSignOut -> {
                _uiState.update { it.copy(dialog = null) }
                signOut()
            }
            AccountBackupUiEvent.DeleteAccount -> {
                if (_uiState.value.runningOperation == null) {
                    _uiState.update { it.copy(dialog = AccountBackupDialog.ConfirmDeleteAccount) }
                }
            }

            AccountBackupUiEvent.ConfirmDeleteAccount -> {
                _uiState.update { it.copy(dialog = null) }
                launchOperation(CloudOperation.DELETE_ACCOUNT) { performDeleteAccount() }
            }

            // A sign-in or sign-out in flight cannot be dismissed: the dialog is its only feedback.
            AccountBackupUiEvent.DismissDialog -> _uiState.update {
                if ((it.dialog as? AccountBackupDialog.AccountChange)?.running == true) it else it.copy(dialog = null)
            }
            AccountBackupUiEvent.Retry -> {
                _uiState.update { it.copy(backup = BackupStatusUiModel.Loading) }
                loadBackupInfo()
            }
        }
    }

    // Collected for the ViewModel's whole life: the account changes when a sign-in finishes, but also when the session
    // is revoked or ended somewhere else.
    private fun observeAccount() {
        viewModelScope.launch {
            observeAccountUseCase().collect { account -> onAccountChanged(account) }
        }
    }

    private fun onAccountChanged(account: Account?) {
        _uiState.update {
            it.copy(
                account = account?.toUiModel(),
                backup = if (account == null) BackupStatusUiModel.Loading else it.backup,
                // The account going null is what a sign-out looks like, and the dialog reporting it must outlive it.
                dialog = if (account == null && it.dialog !is AccountBackupDialog.AccountChange) null else it.dialog
            )
        }
        if (account == null) {
            loadedForAccountId = null
        } else if (account.id != loadedForAccountId) {
            loadedForAccountId = account.id
            loadBackupInfo()
        }
    }

    // The skeleton stays up for at least MIN_BACKUP_LOADING_MILLIS: a backup that arrives sooner would otherwise show
    // it for a few frames and swap it out, which reads as a flicker.
    private fun loadBackupInfo() {
        viewModelScope.launch {
            val minimumLoading = launch { delay(MIN_BACKUP_LOADING_MILLIS) }
            val result = getBackupInfoUseCase()
            minimumLoading.join()
            when (result) {
                is AppResult.Success ->
                    _uiState.update { it.copy(backup = result.data?.toUiModel() ?: BackupStatusUiModel.None) }

                // Unreachable is a state the card shows with a retry, not a message on top of it.
                is AppResult.Failure -> _uiState.update { it.copy(backup = BackupStatusUiModel.Unknown) }
            }
        }
    }

    // The dialog opens once the user has picked in the provider's sheet, so it never sits behind it. Until then the
    // sign-in button's own spinner shows. A closed sheet is the user's own choice and says nothing; any other end is
    // reported in the dialog.
    private fun signIn() {
        launchOperation(CloudOperation.SIGN_IN) {
            val result = signInUseCase(onAccountPicked = {
                _uiState.update { it.copy(dialog = AccountBackupDialog.AccountChange(CloudOperation.SIGN_IN)) }
            })
            when (result) {
                is AppResult.Success -> finishAccountChange(
                    CloudOperation.SIGN_IN,
                    UiText.StringResource(Res.string.account_backup_signed_in_message)
                )

                is AppResult.Failure ->
                    if (result.error == RepositoryError.SignInCancelled) {
                        _uiState.update { it.copy(dialog = null) }
                    } else {
                        finishAccountChange(CloudOperation.SIGN_IN, result.error.toUiText())
                    }
            }
        }
    }

    // The busy flag is raised before the local-data check, so a second tap during that check is ignored too.
    private fun backUp() {
        launchOperation(CloudOperation.BACK_UP) {
            if (hasLocalUserDataUseCase()) {
                performBackUp()
            } else {
                _uiState.update { it.copy(dialog = AccountBackupDialog.ConfirmEmptyBackup) }
            }
        }
    }

    // Like backing up: the busy flag is raised before the local-data check. An empty device has nothing to lose, so it
    // restores without asking.
    private fun restore() {
        launchOperation(CloudOperation.RESTORE) {
            if (hasLocalUserDataUseCase()) {
                _uiState.update { it.copy(dialog = AccountBackupDialog.ConfirmRestore) }
            } else {
                performRestore()
            }
        }
    }

    private fun signOut() {
        launchOperation(CloudOperation.SIGN_OUT, AccountBackupDialog.AccountChange(CloudOperation.SIGN_OUT)) {
            val outcome = when (val result = signOutUseCase()) {
                is AppResult.Success -> UiText.StringResource(Res.string.account_backup_signed_out_message)
                is AppResult.Failure -> result.error.toUiText()
            }
            finishAccountChange(CloudOperation.SIGN_OUT, outcome)
        }
    }

    private fun finishAccountChange(operation: CloudOperation, outcome: UiText) {
        _uiState.update { it.copy(dialog = AccountBackupDialog.AccountChange(operation, outcome)) }
    }

    // The use case signs in again, deletes the backup and then the account. A failure on the way leaves the user signed
    // in with the screen usable, and the backup card shows what is left.
    private suspend fun performDeleteAccount() {
        when (val result = deleteAccountUseCase()) {
            is AppResult.Success -> _uiEffect.send(
                AccountBackupUiEffect.ShowMessage(UiText.StringResource(Res.string.account_backup_account_deleted))
            )

            is AppResult.Failure -> {
                showFailure(result.error)
                // The backup may be gone even though the account is not: read what is there now.
                loadBackupInfo()
            }
        }
    }

    private suspend fun performRestore() {
        when (val result = restoreBackupUseCase()) {
            is AppResult.Success -> {
                _uiState.update { it.copy(backup = result.data.toUiModel()) }
                _uiEffect.send(
                    AccountBackupUiEffect.ShowMessage(UiText.StringResource(Res.string.account_backup_restore_complete))
                )
            }

            is AppResult.Failure -> showFailure(result.error)
        }
    }

    private suspend fun performBackUp() {
        when (val result = backUpUseCase()) {
            is AppResult.Success -> {
                _uiState.update { it.copy(backup = result.data.toUiModel()) }
                _uiEffect.send(
                    AccountBackupUiEffect.ShowMessage(UiText.StringResource(Res.string.account_backup_backup_complete))
                )
            }

            is AppResult.Failure -> showFailure(result.error)
        }
    }

    // A closed sign-in sheet is the user's own choice, so it says nothing. A NotSignedIn failure is shown like any
    // other; the observed account then emits null and the screen goes back to its signed-out state.
    private suspend fun showFailure(error: RepositoryError) {
        if (error == RepositoryError.SignInCancelled) return
        _uiEffect.send(AccountBackupUiEffect.ShowMessage(error.toUiText()))
    }

    // One cloud action at a time. The flag is set before the coroutine starts and cleared in a finally, so a second
    // event is ignored while one runs and a cancellation never leaves the screen stuck busy.
    private fun launchOperation(
        operation: CloudOperation,
        dialog: AccountBackupDialog? = null,
        block: suspend () -> Unit
    ) {
        if (_uiState.value.runningOperation != null) return
        _uiState.update { it.copy(runningOperation = operation, dialog = dialog ?: it.dialog) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(runningOperation = null) }
            }
        }
    }
}
