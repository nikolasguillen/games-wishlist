package com.nikolasguillen.questlog.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.account.GetSignInProviderUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.ObserveAccountUseCase
import com.nikolasguillen.questlog.core.domain.usecase.account.SignInUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.BackUpUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.GetBackupInfoUseCase
import com.nikolasguillen.questlog.core.domain.usecase.backup.HasLocalUserDataUseCase
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
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_backup_complete
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountBackupViewModel(
    private val observeAccountUseCase: ObserveAccountUseCase,
    private val signInUseCase: SignInUseCase,
    getSignInProviderUseCase: GetSignInProviderUseCase,
    private val getBackupInfoUseCase: GetBackupInfoUseCase,
    private val backUpUseCase: BackUpUseCase,
    private val hasLocalUserDataUseCase: HasLocalUserDataUseCase
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

            AccountBackupUiEvent.DismissDialog -> _uiState.update { it.copy(dialog = null) }
            AccountBackupUiEvent.Retry -> loadBackupInfo()
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
                backup = if (account == null) BackupStatusUiModel.Unknown else it.backup,
                dialog = if (account == null) null else it.dialog
            )
        }
        if (account == null) {
            loadedForAccountId = null
        } else if (account.id != loadedForAccountId) {
            loadedForAccountId = account.id
            loadBackupInfo()
        }
    }

    private fun loadBackupInfo() {
        viewModelScope.launch {
            when (val result = getBackupInfoUseCase()) {
                is AppResult.Success ->
                    _uiState.update { it.copy(backup = result.data?.toUiModel() ?: BackupStatusUiModel.None) }

                // Unreachable is a state the card shows with a retry, not a message on top of it.
                is AppResult.Failure -> _uiState.update { it.copy(backup = BackupStatusUiModel.Unknown) }
            }
        }
    }

    private fun signIn() {
        launchOperation(CloudOperation.SIGN_IN) {
            signInUseCase().onFailure { showFailure(it) }
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
    private fun launchOperation(operation: CloudOperation, block: suspend () -> Unit) {
        if (_uiState.value.runningOperation != null) return
        _uiState.update { it.copy(runningOperation = operation) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _uiState.update { it.copy(runningOperation = null) }
            }
        }
    }
}
