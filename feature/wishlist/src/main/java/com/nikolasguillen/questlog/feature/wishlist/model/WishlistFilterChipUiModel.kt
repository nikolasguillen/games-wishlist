package com.nikolasguillen.questlog.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/** One chip of the status filter strip: [filter] is what selecting it applies. */
@Immutable
internal data class WishlistFilterChipUiModel(
    val filter: WishlistStatusFilter,
    val label: UiText,
    val isSelected: Boolean
)
