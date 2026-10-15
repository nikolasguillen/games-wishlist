package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.AuthProvider

/**
 * State of the Account & backup screen.
 *
 * @property signInProvider The one sign-in method this platform offers, which names the sign-in button. The
 * ViewModel sets it at construction; the default only keeps the class constructible.
 * @property account The signed-in account, `null` while signed out.
 * @property backup What the account's cloud backup holds. Meaningful only while [account] is set.
 * @property runningOperation The cloud operation in flight, if any. One at a time: while it is set every cloud action
 * is disabled, so a second tap cannot start a second one.
 * @property dialog The confirmation shown, if any.
 */
@Immutable
internal data class AccountBackupUiState(
    val signInProvider: AuthProvider = AuthProvider.GOOGLE,
    val account: AccountUiModel? = null,
    val backup: BackupStatusUiModel = BackupStatusUiModel.Unknown,
    val runningOperation: CloudOperation? = null,
    val dialog: AccountBackupDialog? = null
)
