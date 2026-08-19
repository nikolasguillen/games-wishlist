package com.example.gameswishlist.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal data class WishlistUiState(
    val listName: UiText = UiText.DynamicString(""),
    val canDeleteList: Boolean = false,
    val contentState: WishlistContentState = WishlistContentState.Loading
)
