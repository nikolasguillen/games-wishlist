package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

/**
 * UI Model for a wishlist list in the list selector bottom sheet.
 *
 * @property id The unique identifier of the wishlist list.
 * @property name The name of the wishlist list.
 * @property iconRes The drawable resource for the wishlist icon.
 * @property isSelected Whether the current game is already in this list.
 */
@Immutable
data class ListSelectorItemUiModel(
    val id: Long,
    val name: UiText,
    val iconRes: DrawableResource,
    val isSelected: Boolean = false
)
