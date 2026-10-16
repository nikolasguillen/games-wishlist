package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface AccountBackupUiEvent {
    data object SignIn : AccountBackupUiEvent
    data object BackUp : AccountBackupUiEvent
    data object Restore : AccountBackupUiEvent
    data object ConfirmRestore : AccountBackupUiEvent
    data object ConfirmEmptyBackup : AccountBackupUiEvent
    data object SignOut : AccountBackupUiEvent

    /** Asks for the confirmation; nothing is deleted until [ConfirmDeleteAccount]. */
    data object DeleteAccount : AccountBackupUiEvent
    data object ConfirmDeleteAccount : AccountBackupUiEvent
    data object DismissDialog : AccountBackupUiEvent

    /** Reads the backup's status again, after it could not be reached. */
    data object Retry : AccountBackupUiEvent
}
