package com.nikolasguillen.questlog.feature.lists.mapper

import com.nikolasguillen.questlog.core.model.WishlistList
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.lists.model.WishlistListUiModel

private const val MAX_DISPLAYED_GAME_COUNT = 100
private const val OVERFLOW_GAME_COUNT_LABEL = "99+"

internal fun WishlistList.toUiModel(): WishlistListUiModel {
    return WishlistListUiModel(
        id = id,
        name = name,
        description = description,
        iconRes = icon.toDrawableRes(),
        coverImagePath = coverImagePath,
        gameCountText = UiText.DynamicString(gameCount.toGameCountLabel())
    )
}

private fun Int.toGameCountLabel(): String {
    return if (this > MAX_DISPLAYED_GAME_COUNT) OVERFLOW_GAME_COUNT_LABEL else toString()
}