package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

/** A confirmation the screen asks for before a cloud action runs. */
@Immutable
internal sealed interface AccountBackupDialog {
    /** Restoring replaces the lists, saved games and owned platforms the device already has, and cannot be undone. */
    data object ConfirmRestore : AccountBackupDialog

    /** Backing up a device with nothing on it would replace a real backup with an empty one. */
    data object ConfirmEmptyBackup : AccountBackupDialog

    /** Deleting removes the account and its cloud backup for good. The data on the device stays. */
    data object ConfirmDeleteAccount : AccountBackupDialog
}
