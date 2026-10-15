package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

/** A confirmation the screen asks for before a cloud action runs. */
@Immutable
internal sealed interface AccountBackupDialog {
    /** Backing up a device with nothing on it would replace a real backup with an empty one. */
    data object ConfirmEmptyBackup : AccountBackupDialog
}
