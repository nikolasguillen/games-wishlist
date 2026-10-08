package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface SettingsUiEffect {
    /** The user tapped the translation model's download action while off an unmetered network. */
    data object ShowWifiRequiredDialog : SettingsUiEffect

    /** The user tapped the translation model's download action on an unmetered network. */
    data object ShowDownloadConfirmDialog : SettingsUiEffect
}
