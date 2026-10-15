package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

/**
 * State of the Account & backup screen.
 *
 * @property account The signed-in account, `null` while signed out.
 * @property backup What the account's cloud backup holds. Meaningful only while [account] is set.
 * @property runningOperation The cloud operation in flight, if any. One at a time: while it is set every cloud action
 * is disabled, so a second tap cannot start a second one.
 * @property dialog The confirmation shown, if any.
 */
@Immutable
internal data class AccountBackupUiState(
    val account: AccountUiModel? = null,
    val backup: BackupStatusUiModel = BackupStatusUiModel.Unknown,
    val runningOperation: CloudOperation? = null,
    val dialog: AccountBackupDialog? = null
)
