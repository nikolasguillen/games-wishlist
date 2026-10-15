package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface AccountBackupUiEvent {
    data object SignIn : AccountBackupUiEvent
    data object BackUp : AccountBackupUiEvent
    data object ConfirmEmptyBackup : AccountBackupUiEvent
    data object DismissDialog : AccountBackupUiEvent

    /** Reads the backup's status again, after it could not be reached. */
    data object Retry : AccountBackupUiEvent
}
