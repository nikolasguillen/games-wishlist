package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

/**
 * UI model representing where and when a game is (or will be) available.
 *
 * @property mainDate The primary release date to display.
 * @property platforms Compact platform tiles to display alongside the main date.
 * @property detailedDates Per-platform release dates for the expanded dialog view.
 * @property isExpandable Whether there are multiple different release dates that justify a detailed view.
 */
data class AvailabilityUiModel(
    val mainDate: UiText,
    val platforms: List<PlatformTileUiModel>,
    val detailedDates: List<PlatformReleaseDateUiModel>,
    val isExpandable: Boolean
)
