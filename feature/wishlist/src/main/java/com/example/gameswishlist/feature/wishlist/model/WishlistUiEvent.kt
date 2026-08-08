package com.example.gameswishlist.feature.wishlist.model

internal sealed interface WishlistUiEvent {
    data object OnWishlistDeleted : WishlistUiEvent
}