package com.nikolasguillen.questlog.feature.wishlist.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface WishlistUiEffect {
    data object NavigateBack : WishlistUiEffect
    data class ShowSnackbar(val message: UiText) : WishlistUiEffect
}