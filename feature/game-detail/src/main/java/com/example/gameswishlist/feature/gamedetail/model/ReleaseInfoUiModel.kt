package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

/**
 * UI model representing release information for a game.
 *
 * @property mainDate The primary release date to display.
 * @property detailedMessage A detailed message with additional release dates if available.
 * @property isExpandable Whether there are multiple different release dates that justify a detailed view.
 */
data class ReleaseInfoUiModel(
    val mainDate: UiText,
    val detailedMessage: UiText?,
    val isExpandable: Boolean
)
