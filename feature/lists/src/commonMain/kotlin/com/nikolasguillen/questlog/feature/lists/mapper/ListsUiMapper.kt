package com.nikolasguillen.questlog.feature.lists.mapper

import com.nikolasguillen.questlog.core.domain.model.WishlistSummary
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.lists.model.WishlistListUiModel

private const val MAX_DISPLAYED_GAME_COUNT = 100
private const val OVERFLOW_GAME_COUNT_LABEL = "99+"

internal fun WishlistSummary.toUiModel(): WishlistListUiModel {
    return WishlistListUiModel(
        id = list.id,
        name = list.name,
        description = list.description,
        iconRes = list.icon.toDrawableRes(),
        coverImagePath = list.coverImagePath,
        gameCountText = UiText.DynamicString(list.gameCount.toGameCountLabel()),
        isDefault = isDefault
    )
}

private fun Int.toGameCountLabel(): String {
    return if (this > MAX_DISPLAYED_GAME_COUNT) OVERFLOW_GAME_COUNT_LABEL else toString()
}