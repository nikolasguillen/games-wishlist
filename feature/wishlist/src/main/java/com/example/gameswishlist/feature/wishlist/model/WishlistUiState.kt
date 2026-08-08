package com.example.gameswishlist.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
data class WishlistUiState(
    val listName: UiText = UiText.DynamicString(""),
    val sections: List<WishlistSectionUiModel> = emptyList(),
    val canDeleteList: Boolean = false
)
