package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.model.ListSelectorItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

internal fun WishlistAssignment.toSelectorItem(): ListSelectorItemUiModel {
    return ListSelectorItemUiModel(
        id = list.id,
        name = UiText.DynamicString(list.name),
        iconRes = list.icon.toDrawableRes(),
        isSelected = isAssigned
    )
}
