package com.nikolasguillen.questlog.core.ui.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/**
 * UI Model for a wishlist list in the list selector bottom sheet.
 *
 * @property id The unique identifier of the wishlist list.
 * @property name The name of the wishlist list.
 * @property iconRes The drawable resource ID for the wishlist icon.
 * @property isSelected Whether the current game is already in this list.
 */
@Immutable
data class ListSelectorItemUiModel(
    val id: Long,
    val name: UiText,
    @DrawableRes val iconRes: Int,
    val isSelected: Boolean = false
)
