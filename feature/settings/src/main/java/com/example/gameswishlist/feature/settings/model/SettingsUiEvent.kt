package com.example.gameswishlist.feature.settings.model

internal sealed interface SettingsUiEvent {
    data class SetDescriptionTranslation(val enabled: Boolean) : SettingsUiEvent
}
