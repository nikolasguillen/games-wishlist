package com.example.gameswishlist.feature.lists.model

import androidx.annotation.DrawableRes
import com.example.gameswishlist.core.ui.model.UiText

internal data class WishlistListUiModel(
    val id: Long,
    val name: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    val gameCountText: UiText
)