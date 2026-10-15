package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/** What the account's cloud backup holds, as the backup card shows it. */
@Immutable
internal sealed interface BackupStatusUiModel {
    /** Still loading, or the backup could not be reached. */
    data object Unknown : BackupStatusUiModel

    /** Signed in with no backup yet. */
    data object None : BackupStatusUiModel

    /**
     * @property lastBackup When the backup was made, already formatted for the device.
     * @property summary How many lists and games it holds.
     */
    data class Available(val lastBackup: UiText, val summary: UiText) : BackupStatusUiModel
}
