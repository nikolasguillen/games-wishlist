package com.nikolasguillen.questlog.feature.wishlist.model

internal sealed interface WishlistUiEvent {
    data object OnSetAsDefault : WishlistUiEvent
    data object OnWishlistDeleted : WishlistUiEvent
    data class OnGameRemoved(val gameId: Int) : WishlistUiEvent
}