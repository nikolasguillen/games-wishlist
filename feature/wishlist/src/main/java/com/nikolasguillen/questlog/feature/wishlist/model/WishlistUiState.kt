package com.nikolasguillen.questlog.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class WishlistUiState(
    val listName: UiText = UiText.DynamicString(""),
    val canDeleteList: Boolean = false,
    val contentState: WishlistContentState = WishlistContentState.Loading
)
