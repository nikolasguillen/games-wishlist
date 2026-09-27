package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface SettingsUiEvent {
    /** The user tapped the translation model row (or its retry action) to start a download. */
    data object DownloadTranslationModel : SettingsUiEvent

    /** The user confirmed the download from [SettingsUiEffect.ShowDownloadConfirmDialog]. */
    data object ConfirmDownloadTranslationModel : SettingsUiEvent

    /** Dispatched from the composition-only permission read whenever it changes (e.g. on `ON_RESUME`). */
    data class NotificationPermissionChanged(val canDeliver: Boolean, val isPermanentlyDenied: Boolean) :
        SettingsUiEvent
}
