package com.example.gameswishlist.feature.gamedetail.model

/**
 * Represents the state of the wishlist selection process.
 * If this state is present (non-null) in the UI state, the selector should be displayed.
 *
 * @property availableLists The list of all wishlist categories available for the user.
 * @property selectedListIds The IDs of the lists currently selected in the UI (temporary state).
 */
data class WishlistSelectorState(
    val availableLists: List<WishlistListUiModel> = emptyList(),
    val selectedListIds: Set<Long> = emptySet()
)
