package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

/**
 * UI Model for a wishlist list in the selector dialog.
 *
 * @property id The unique identifier of the wishlist list.
 * @property name The name of the wishlist list.
 * @property isSelected Whether the current game is already in this list.
 */
data class WishlistListUiModel(
    val id: Long,
    val name: UiText,
    val isSelected: Boolean = false
)
