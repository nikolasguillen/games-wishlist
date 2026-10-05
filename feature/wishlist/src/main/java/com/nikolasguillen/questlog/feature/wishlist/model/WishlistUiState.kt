package com.nikolasguillen.questlog.feature.wishlist.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class WishlistUiState(
    val listName: UiText = UiText.DynamicString(""),
    val description: String? = null,
    @DrawableRes val iconRes: Int = null.toDrawableRes(),
    val coverImagePath: String? = null,
    val gameCountText: UiText = UiText.DynamicString(""),
    val isDefaultList: Boolean = false,
    val showListOptions: Boolean = false,
    val contentState: WishlistContentState = WishlistContentState.Loading
)
