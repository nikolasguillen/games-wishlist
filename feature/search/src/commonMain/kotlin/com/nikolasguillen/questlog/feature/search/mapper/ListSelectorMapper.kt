package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.domain.model.WishlistAssignment
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.ListSelectorItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

internal fun WishlistAssignment.toSelectorItem(): ListSelectorItemUiModel {
    return ListSelectorItemUiModel(
        id = list.id,
        name = UiText.DynamicString(list.name),
        iconRes = list.icon.toDrawableRes(),
        isSelected = isAssigned
    )
}
