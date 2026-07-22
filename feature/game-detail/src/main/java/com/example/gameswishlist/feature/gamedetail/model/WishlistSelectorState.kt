package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

/**
 * Represents the state of the wishlist selection process.
 * If this state is present (non-null) in the UI state, the selector should be displayed.
 *
 * @property gameName The name of the game being added to a list.
 * @property availableLists The list of all wishlist categories available for the user (working state).
 */
internal data class WishlistSelectorState(
    val gameName: UiText,
    val availableLists: List<WishlistListUiModel> = emptyList()
)
