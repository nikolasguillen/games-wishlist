package com.example.gameswishlist.feature.lists.mapper

import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.lists.model.WishlistListUiModel

private const val MAX_DISPLAYED_GAME_COUNT = 100
private const val OVERFLOW_GAME_COUNT_LABEL = "99+"

internal fun WishlistList.toUiModel(): WishlistListUiModel {
    return WishlistListUiModel(
        id = id,
        name = name,
        description = description,
        iconRes = icon.toDrawableRes(),
        gameCountText = UiText.DynamicString(gameCount.toGameCountLabel())
    )
}

private fun Int.toGameCountLabel(): String {
    return if (this > MAX_DISPLAYED_GAME_COUNT) OVERFLOW_GAME_COUNT_LABEL else toString()
}