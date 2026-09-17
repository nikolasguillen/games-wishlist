package com.example.gameswishlist.feature.settings.model

internal sealed interface SettingsUiEvent {
    data object DownloadTranslationModel : SettingsUiEvent
}
