package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

/** What the Settings hub shows over itself after the user taps the translation model's download action. */
@Immutable
internal sealed interface SettingsDialog {
    /** The device is not on an unmetered network, so the download is refused rather than left stuck at 0%. */
    data object WifiRequired : SettingsDialog

    /** The device is on an unmetered network: the model is several gigabytes, so the user confirms first. */
    data object ConfirmTranslationModelDownload : SettingsDialog
}
