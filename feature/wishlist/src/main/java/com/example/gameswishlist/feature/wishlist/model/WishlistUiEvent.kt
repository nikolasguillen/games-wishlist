package com.example.gameswishlist.feature.wishlist.model

sealed interface WishlistUiEvent {
    data object OnWishlistDeleted : WishlistUiEvent
}