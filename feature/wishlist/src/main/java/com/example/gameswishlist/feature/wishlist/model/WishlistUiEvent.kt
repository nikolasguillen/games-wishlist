package com.example.gameswishlist.feature.wishlist.model

internal sealed interface WishlistUiEvent {
    data object OnWishlistDeleted : WishlistUiEvent
    data class OnGameRemoved(val gameId: Int) : WishlistUiEvent
    data class OnGameRemoveUndo(val gameId: Int) : WishlistUiEvent
}