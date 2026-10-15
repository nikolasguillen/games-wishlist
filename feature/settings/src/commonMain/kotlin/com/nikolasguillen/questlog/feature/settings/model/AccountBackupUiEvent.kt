package com.nikolasguillen.questlog.feature.settings.model

import com.nikolasguillen.questlog.core.model.AuthProvider

internal sealed interface AccountBackupUiEvent {
    data class SignIn(val provider: AuthProvider) : AccountBackupUiEvent
    data object BackUp : AccountBackupUiEvent
    data object ConfirmEmptyBackup : AccountBackupUiEvent
    data object DismissDialog : AccountBackupUiEvent

    /** Reads the backup's status again, after it could not be reached. */
    data object Retry : AccountBackupUiEvent
}
