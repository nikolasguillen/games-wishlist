package com.nikolasguillen.questlog.feature.settings

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
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.model.BackupInfo
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupDialog
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEffect
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEvent
import com.nikolasguillen.questlog.feature.settings.model.BackupStatusUiModel
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_account_deleted
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signed_in_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signed_out_message
import com.nikolasguillen.questlog.core.ui.model.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

/**
 * Covers the Account & backup screen's state machine: signed out and signed in, the backup status that follows a
 * sign-in, a closed sign-in sheet saying nothing while any other failure says its cause, the empty-device
 * confirmation before a backup, and the one-operation-at-a-time rule, including that the busy flag is cleared after a
 * success, a failure and a cancellation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountBackupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val account = MutableStateFlow<Account?>(null)
    private val observeAccountUseCase = mockk<ObserveAccountUseCase>()
    private val signInUseCase = mockk<SignInUseCase>()
    private val getSignInProviderUseCase = mockk<GetSignInProviderUseCase>()
    private val getBackupInfoUseCase = mockk<GetBackupInfoUseCase>()
    private val backUpUseCase = mockk<BackUpUseCase>()
    private val restoreBackupUseCase = mockk<RestoreBackupUseCase>()
    private val hasLocalUserDataUseCase = mockk<HasLocalUserDataUseCase>()
    private val signOutUseCase = mockk<SignOutUseCase>()
    private val deleteAccountUseCase = mockk<DeleteAccountUseCase>()

    private val signedIn = Account(id = "uid-1", email = "ada@example.com", providers = setOf(AuthProvider.GOOGLE))
    private val info = BackupInfo(createdAt = Instant.fromEpochMilliseconds(1_750_000_000_000L), listCount = 2, gameCount = 5)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeAccountUseCase() } returns account
        every { getSignInProviderUseCase() } returns AuthProvider.GOOGLE
        coEvery { getBackupInfoUseCase() } returns AppResult.success(null)
        coEvery { hasLocalUserDataUseCase() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AccountBackupViewModel(
        observeAccountUseCase, signInUseCase, getSignInProviderUseCase, getBackupInfoUseCase, backUpUseCase, restoreBackupUseCase,
        hasLocalUserDataUseCase, signOutUseCase, deleteAccountUseCase
    )

    private fun kotlinx.coroutines.test.TestScope.collectEffects(viewModel: AccountBackupViewModel): List<AccountBackupUiEffect> {
        val effects = mutableListOf<AccountBackupUiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEffect.collect { effects += it } }
        return effects
    }

    @Test
    fun `the screen names the sign-in method the platform offers`() = runTest(testDispatcher) {
        every { getSignInProviderUseCase() } returns AuthProvider.APPLE

        assertEquals(AuthProvider.APPLE, viewModel().uiState.value.signInProvider)
    }

    @Test
    fun `nobody signed in leaves the screen signed out`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.account)
        assertEquals(BackupStatusUiModel.Unknown, viewModel.uiState.value.backup)
        coVerify(exactly = 0) { getBackupInfoUseCase() }
    }

    @Test
    fun `signing in succeeds and shows the account`() = runTest(testDispatcher) {
        coEvery { signInUseCase(any()) } coAnswers {
            account.value = signedIn
            AppResult.success(signedIn)
        }
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)

        viewModel.onEvent(AccountBackupUiEvent.SignIn)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ada@example.com", state.account?.email)
        assertEquals(listOf(AuthProvider.GOOGLE), state.account?.linkedProviders)
        assertNull(state.runningOperation)
        assertTrue(effects.isEmpty())
        assertEquals(
            AccountBackupDialog.AccountChange(
                CloudOperation.SIGN_IN,
                UiText.StringResource(Res.string.account_backup_signed_in_message)
            ),
            state.dialog
        )
    }

    @Test
    fun `once the account is picked a dialog shows that cannot be dismissed until sign-in ends`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { signInUseCase(any()) } coAnswers {
            firstArg<() -> Unit>().invoke()
            gate.await()
            account.value = signedIn
            AppResult.success(signedIn)
        }
        val viewModel = viewModel()

        viewModel.onEvent(AccountBackupUiEvent.SignIn)
        advanceUntilIdle()
        assertEquals(AccountBackupDialog.AccountChange(CloudOperation.SIGN_IN), viewModel.uiState.value.dialog)

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)
        assertEquals(AccountBackupDialog.AccountChange(CloudOperation.SIGN_IN), viewModel.uiState.value.dialog)

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value.dialog as AccountBackupDialog.AccountChange).outcome != null)

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `no dialog shows while the user is still choosing an account`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { signInUseCase(any()) } coAnswers {
            gate.await()
            AppResult.failure(RepositoryError.SignInCancelled)
        }
        val viewModel = viewModel()

        viewModel.onEvent(AccountBackupUiEvent.SignIn)
        advanceUntilIdle()

        assertEquals(CloudOperation.SIGN_IN, viewModel.uiState.value.runningOperation)
        assertNull(viewModel.uiState.value.dialog)

        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun `a failed sign-in shows its cause`() = runTest(testDispatcher) {
        coEvery { signInUseCase(any()) } returns AppResult.failure(RepositoryError.AccountExistsWithOtherMethod)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)

        viewModel.onEvent(AccountBackupUiEvent.SignIn)
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        assertEquals(
            AccountBackupDialog.AccountChange(CloudOperation.SIGN_IN, RepositoryError.AccountExistsWithOtherMethod.toUiText()),
            viewModel.uiState.value.dialog
        )
        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `a closed sign-in sheet closes the dialog and says nothing`() = runTest(testDispatcher) {
        coEvery { signInUseCase(any()) } returns AppResult.failure(RepositoryError.SignInCancelled)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)

        viewModel.onEvent(AccountBackupUiEvent.SignIn)
        advanceUntilIdle()

        assertTrue(effects.isEmpty())
        assertNull(viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `signed in with no backup shows none`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        assertEquals(BackupStatusUiModel.None, viewModel.uiState.value.backup)
    }

    @Test
    fun `signed in with a backup shows it`() = runTest(testDispatcher) {
        coEvery { getBackupInfoUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        val backup = viewModel.uiState.value.backup
        assertTrue(backup is BackupStatusUiModel.Available)
    }

    @Test
    fun `an unreachable backup shows unknown and retry reads it again`() = runTest(testDispatcher) {
        coEvery { getBackupInfoUseCase() } returns AppResult.failure(RepositoryError.NoNetwork)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()
        assertEquals(BackupStatusUiModel.Unknown, viewModel.uiState.value.backup)

        coEvery { getBackupInfoUseCase() } returns AppResult.success(null)
        viewModel.onEvent(AccountBackupUiEvent.Retry)
        advanceUntilIdle()

        assertEquals(BackupStatusUiModel.None, viewModel.uiState.value.backup)
    }

    @Test
    fun `back up with local data runs the backup directly`() = runTest(testDispatcher) {
        coEvery { backUpUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        coVerify(exactly = 1) { backUpUseCase() }
        assertNull(viewModel.uiState.value.dialog)
        assertTrue(viewModel.uiState.value.backup is BackupStatusUiModel.Available)
        assertEquals(1, effects.size)
        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `back up with no local data asks first and runs nothing until confirmed`() = runTest(testDispatcher) {
        coEvery { hasLocalUserDataUseCase() } returns false
        coEvery { backUpUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        assertEquals(AccountBackupDialog.ConfirmEmptyBackup, viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
        coVerify(exactly = 0) { backUpUseCase() }

        viewModel.onEvent(AccountBackupUiEvent.ConfirmEmptyBackup)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 1) { backUpUseCase() }
    }

    @Test
    fun `dismissing the dialog closes it and runs nothing`() = runTest(testDispatcher) {
        coEvery { hasLocalUserDataUseCase() } returns false
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)

        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 0) { backUpUseCase() }
    }

    @Test
    fun `the busy flag is set while a backup runs and a second tap is ignored`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { backUpUseCase() } coAnswers {
            gate.await()
            AppResult.success(info)
        }
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()
        assertEquals(CloudOperation.BACK_UP, viewModel.uiState.value.runningOperation)

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { backUpUseCase() }
        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `the busy flag is cleared after a failed backup`() = runTest(testDispatcher) {
        coEvery { backUpUseCase() } returns AppResult.failure(RepositoryError.NoNetwork)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.runningOperation)
        assertEquals(listOf<AccountBackupUiEffect>(AccountBackupUiEffect.ShowMessage(RepositoryError.NoNetwork.toUiText())), effects)
    }

    @Test
    fun `the busy flag is cleared after a cancelled backup`() = runTest(testDispatcher) {
        coEvery { backUpUseCase() } coAnswers { throw CancellationException("scope closed") }
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `a backup that finds nobody signed in shows the message`() = runTest(testDispatcher) {
        coEvery { backUpUseCase() } returns AppResult.failure(RepositoryError.NotSignedIn)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertNotNull(effects.single())
        assertEquals(AccountBackupUiEffect.ShowMessage(RepositoryError.NotSignedIn.toUiText()), effects.single())
    }

    @Test
    fun `signing out elsewhere returns the screen to signed out`() = runTest(testDispatcher) {
        coEvery { getBackupInfoUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        account.value = null
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.account)
        assertEquals(BackupStatusUiModel.Unknown, viewModel.uiState.value.backup)
    }

    // ---- restore ----

    @Test
    fun `restore with local data asks first and runs nothing`() = runTest(testDispatcher) {
        coEvery { restoreBackupUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.Restore)
        advanceUntilIdle()

        assertEquals(AccountBackupDialog.ConfirmRestore, viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
        coVerify(exactly = 0) { restoreBackupUseCase() }
    }

    @Test
    fun `dismissing the restore dialog leaves the use case uncalled`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.Restore)
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 0) { restoreBackupUseCase() }
    }

    @Test
    fun `confirming the restore runs it, shows the result and refreshes the backup`() = runTest(testDispatcher) {
        coEvery { restoreBackupUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.Restore)
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.ConfirmRestore)
        advanceUntilIdle()

        coVerify(exactly = 1) { restoreBackupUseCase() }
        assertNull(viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
        assertTrue(viewModel.uiState.value.backup is BackupStatusUiModel.Available)
        assertEquals(1, effects.size)
    }

    @Test
    fun `restore on an empty device runs without asking`() = runTest(testDispatcher) {
        coEvery { hasLocalUserDataUseCase() } returns false
        coEvery { restoreBackupUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.Restore)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 1) { restoreBackupUseCase() }
    }

    @Test
    fun `each restore failure shows its own text`() = runTest(testDispatcher) {
        coEvery { hasLocalUserDataUseCase() } returns false
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        listOf(
            RepositoryError.BackupNotFound,
            RepositoryError.BackupFromNewerVersion,
            RepositoryError.BackupUnreadable
        ).forEach { error ->
            coEvery { restoreBackupUseCase() } returns AppResult.failure(error)
            viewModel.onEvent(AccountBackupUiEvent.Restore)
            advanceUntilIdle()
        }

        assertEquals(
            listOf<AccountBackupUiEffect>(
                AccountBackupUiEffect.ShowMessage(RepositoryError.BackupNotFound.toUiText()),
                AccountBackupUiEffect.ShowMessage(RepositoryError.BackupFromNewerVersion.toUiText()),
                AccountBackupUiEffect.ShowMessage(RepositoryError.BackupUnreadable.toUiText())
            ),
            effects
        )
        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `a running restore blocks a backup and the busy flag is cleared after a cancellation`() = runTest(testDispatcher) {
        coEvery { hasLocalUserDataUseCase() } returns false
        val gate = CompletableDeferred<Unit>()
        coEvery { restoreBackupUseCase() } coAnswers {
            gate.await()
            throw CancellationException("scope closed")
        }
        coEvery { backUpUseCase() } returns AppResult.success(info)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.Restore)
        advanceUntilIdle()
        assertEquals(CloudOperation.RESTORE, viewModel.uiState.value.runningOperation)
        viewModel.onEvent(AccountBackupUiEvent.BackUp)
        advanceUntilIdle()
        coVerify(exactly = 0) { backUpUseCase() }

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.runningOperation)
    }

    @Test
    fun `signing out runs the use case and reports it in a dialog that outlives the account`() = runTest(testDispatcher) {
        coEvery { signOutUseCase() } coAnswers {
            account.value = null
            AppResult.success(Unit)
        }
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.SignOut)
        viewModel.onEvent(AccountBackupUiEvent.ConfirmSignOut)
        advanceUntilIdle()

        coVerify(exactly = 1) { signOutUseCase() }
        coVerify(exactly = 0) { deleteAccountUseCase() }
        assertNull(viewModel.uiState.value.account)
        assertNull(viewModel.uiState.value.runningOperation)
        assertTrue(effects.isEmpty())
        assertEquals(
            AccountBackupDialog.AccountChange(
                CloudOperation.SIGN_OUT,
                UiText.StringResource(Res.string.account_backup_signed_out_message)
            ),
            viewModel.uiState.value.dialog
        )
    }

    @Test
    fun `sign out asks first and signs nobody out until confirmed`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.SignOut)
        advanceUntilIdle()

        assertEquals(AccountBackupDialog.ConfirmSignOut, viewModel.uiState.value.dialog)
        coVerify(exactly = 0) { signOutUseCase() }

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)
        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 0) { signOutUseCase() }
    }

    @Test
    fun `a failed sign out shows its cause and keeps the account`() = runTest(testDispatcher) {
        coEvery { signOutUseCase() } returns AppResult.failure(RepositoryError.NoNetwork)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.SignOut)
        viewModel.onEvent(AccountBackupUiEvent.ConfirmSignOut)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.account)
        assertNull(viewModel.uiState.value.runningOperation)
        assertTrue(effects.isEmpty())
        assertEquals(
            AccountBackupDialog.AccountChange(CloudOperation.SIGN_OUT, RepositoryError.NoNetwork.toUiText()),
            viewModel.uiState.value.dialog
        )
    }

    @Test
    fun `delete account asks first and runs nothing`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()

        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)
        advanceUntilIdle()

        assertEquals(AccountBackupDialog.ConfirmDeleteAccount, viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
        coVerify(exactly = 0) { deleteAccountUseCase() }
    }

    @Test
    fun `dismissing the delete dialog leaves the use case uncalled`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)

        viewModel.onEvent(AccountBackupUiEvent.DismissDialog)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.dialog)
        coVerify(exactly = 0) { deleteAccountUseCase() }
    }

    @Test
    fun `confirming the delete runs it, ends signed out and says so`() = runTest(testDispatcher) {
        coEvery { deleteAccountUseCase() } coAnswers {
            account.value = null
            AppResult.success(Unit)
        }
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)

        viewModel.onEvent(AccountBackupUiEvent.ConfirmDeleteAccount)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteAccountUseCase() }
        assertNull(viewModel.uiState.value.account)
        assertNull(viewModel.uiState.value.dialog)
        assertNull(viewModel.uiState.value.runningOperation)
        assertEquals(
            listOf<AccountBackupUiEffect>(
                AccountBackupUiEffect.ShowMessage(UiText.StringResource(Res.string.account_backup_account_deleted))
            ),
            effects
        )
    }

    @Test
    fun `a failed delete shows its cause, stays signed in and reads the backup again`() = runTest(testDispatcher) {
        coEvery { deleteAccountUseCase() } returns AppResult.failure(RepositoryError.NoNetwork)
        val viewModel = viewModel()
        val effects = collectEffects(viewModel)
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)

        viewModel.onEvent(AccountBackupUiEvent.ConfirmDeleteAccount)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.account)
        assertNull(viewModel.uiState.value.runningOperation)
        assertEquals(
            listOf<AccountBackupUiEffect>(AccountBackupUiEffect.ShowMessage(RepositoryError.NoNetwork.toUiText())),
            effects
        )
        // Once when the account appeared, once after the failure: the backup may be gone though the account is not.
        coVerify(exactly = 2) { getBackupInfoUseCase() }
    }

    @Test
    fun `a running delete blocks sign out and the busy flag is cleared after a cancellation`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { deleteAccountUseCase() } coAnswers {
            gate.await()
            throw CancellationException("scope closed")
        }
        coEvery { signOutUseCase() } returns AppResult.success(Unit)
        val viewModel = viewModel()
        account.value = signedIn
        advanceUntilIdle()
        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)
        viewModel.onEvent(AccountBackupUiEvent.ConfirmDeleteAccount)
        advanceUntilIdle()
        assertEquals(CloudOperation.DELETE_ACCOUNT, viewModel.uiState.value.runningOperation)

        viewModel.onEvent(AccountBackupUiEvent.SignOut)
        viewModel.onEvent(AccountBackupUiEvent.DeleteAccount)
        advanceUntilIdle()
        coVerify(exactly = 0) { signOutUseCase() }
        assertNull(viewModel.uiState.value.dialog)

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.runningOperation)
    }
}
