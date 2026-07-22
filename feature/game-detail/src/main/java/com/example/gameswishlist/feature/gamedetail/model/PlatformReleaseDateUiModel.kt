package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.ui.graphics.Color
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A single row in the expanded per-platform release dates dialog.
 *
 * @property platformId The platform's identifier.
 * @property platformName The platform's full display name.
 * @property code A short, display-ready code (max 3 characters), matching the platform tile strip.
 * @property color A distinguishing background color for the tile, matching the platform tile strip.
 * @property date The formatted release date for this platform.
 */
internal data class PlatformReleaseDateUiModel(
    val platformId: Int,
    val platformName: UiText,
    val code: UiText,
    val color: Color,
    val date: UiText
)
