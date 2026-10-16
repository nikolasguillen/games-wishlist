package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/** What the screen shows over itself: a confirmation before a cloud action runs, or the progress of a sign-in, sign-out or account deletion. */
@Immutable
internal sealed interface AccountBackupDialog {
    /** Restoring replaces the lists, saved games and owned platforms the device already has, and cannot be undone. */
    data object ConfirmRestore : AccountBackupDialog

    /** Backing up a device with nothing on it would replace a real backup with an empty one. */
    data object ConfirmEmptyBackup : AccountBackupDialog

    /** Signing out only ends the session: the device's data and the cloud backup stay. It is asked because it is one tap away. */
    data object ConfirmSignOut : AccountBackupDialog

    /** Deleting removes the account and its cloud backup for good. The data on the device stays. */
    data object ConfirmDeleteAccount : AccountBackupDialog

    /**
     * A sign-in, sign-out or account deletion, from start to finish. While [outcome] is `null` it is running: the dialog shows a loader and
     * cannot be dismissed. Once the operation ends [outcome] says how it went and the dialog is closed with a button.
     */
    data class AccountChange(val operation: CloudOperation, val outcome: UiText? = null) : AccountBackupDialog {
        val running: Boolean get() = outcome == null
    }
}
