package com.example.gameswishlist.feature.settings.model

internal sealed interface SettingsUiEffect {
    /** The user tapped the translation model's download action while off an unmetered network. */
    data object ShowWifiRequiredDialog : SettingsUiEffect
}
