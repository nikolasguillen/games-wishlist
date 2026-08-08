package com.example.gameswishlist.feature.wishlist.model

import com.example.gameswishlist.core.ui.model.UiText

internal sealed interface WishlistUiEffect {
    data object NavigateBack : WishlistUiEffect
    data class ShowSnackbar(
        val message: UiText,
        val actionLabel: UiText? = null,
        val actionEvent: WishlistUiEvent? = null
    ) : WishlistUiEffect
}