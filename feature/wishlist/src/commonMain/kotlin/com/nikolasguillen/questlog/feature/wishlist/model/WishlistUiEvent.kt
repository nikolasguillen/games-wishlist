package com.nikolasguillen.questlog.feature.wishlist.model

import com.nikolasguillen.questlog.core.ui.model.WishlistFormUiModel

internal sealed interface WishlistUiEvent {
    data object OnSetAsDefault : WishlistUiEvent
    data object OnWishlistDeleted : WishlistUiEvent
    data class OnGameRemoved(val gameId: Int) : WishlistUiEvent
    data class OnListEdited(val values: WishlistFormUiModel) : WishlistUiEvent
    data object OnViewModeToggled : WishlistUiEvent
    data class OnStatusFilterSelected(val filter: WishlistStatusFilter) : WishlistUiEvent
}