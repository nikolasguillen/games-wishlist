package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface SettingsUiEvent {
    /** The user tapped the translation model row (or its retry action) to start a download. */
    data object DownloadTranslationModel : SettingsUiEvent

    /** The user confirmed the download from [SettingsUiEffect.ShowDownloadConfirmDialog]. */
    data object ConfirmDownloadTranslationModel : SettingsUiEvent
}
