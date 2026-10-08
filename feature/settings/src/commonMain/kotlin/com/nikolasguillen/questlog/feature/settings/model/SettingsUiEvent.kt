package com.nikolasguillen.questlog.feature.settings.model

import com.nikolasguillen.questlog.core.model.AppearanceMode

internal sealed interface SettingsUiEvent {
    /** The user tapped the translation model row (or its retry action) to start a download. */
    data object DownloadTranslationModel : SettingsUiEvent

    /** The user confirmed the download from [SettingsUiEffect.ShowDownloadConfirmDialog]. */
    data object ConfirmDownloadTranslationModel : SettingsUiEvent

    /** Dispatched from the composition-only permission read whenever it changes (e.g. on `ON_RESUME`). */
    data class NotificationPermissionChanged(val canDeliver: Boolean, val isPermanentlyDenied: Boolean) :
        SettingsUiEvent

    /** The user picked a different Appearance option from the segmented control. */
    data class AppearanceModeChanged(val mode: AppearanceMode) : SettingsUiEvent
}
