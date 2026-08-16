package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A compact visual representation of a platform, for icon-tile display.
 *
 * @property id The platform's identifier.
 * @property code A short, display-ready code (max 3 characters).
 * @property color A distinguishing background color for the tile.
 */
@Immutable
internal data class PlatformTileUiModel(
    val id: Int,
    val code: UiText,
    val color: Color
)
