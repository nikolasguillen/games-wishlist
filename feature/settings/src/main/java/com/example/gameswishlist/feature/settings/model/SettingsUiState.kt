package com.example.gameswishlist.feature.settings.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

/**
 * State of the Settings hub.
 *
 * There is no `ContentState` here on purpose: the groups and rows are static, and the only value that
 * has to be read is [ownedPlatformsSummary]. A four-state lifecycle around a single subtitle would add
 * a `when` that always lands on the same branch.
 *
 * @property ownedPlatformsSummary The platforms the filter currently runs on, `null` while unknown so
 * the row shows a title with no subtitle instead of flashing a wrong one.
 * @property appVersion The installed version name, read once at construction.
 * @property isTranslationSupported Whether this device can translate descriptions on-device at all.
 * Starts `false` and flips once the async support check resolves — the row is absent for that first
 * beat rather than shown and then withdrawn on an unsupported device.
 * @property isTranslationEnabled Whether the user switched translation on. Meaningless while
 * [isTranslationSupported] is `false`, since the row is not shown at all in that case.
 */
@Immutable
internal data class SettingsUiState(
    val ownedPlatformsSummary: UiText? = null,
    val appVersion: String = "",
    val isTranslationSupported: Boolean = false,
    val isTranslationEnabled: Boolean = false
)
