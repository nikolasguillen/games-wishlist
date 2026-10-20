package com.nikolasguillen.questlog.feature.wishlist.model

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.model.WishlistFormUiModel

/**
 * @property gamePendingRemoval The game the remove confirmation is open for, or `null` when it is closed. It is
 * state rather than a flag in the screen because the dialog needs the whole game to word itself and has to
 * survive a rotation; the list is the ViewModel's, so the ViewModel derives the game from it.
 */
@Immutable
internal data class WishlistUiState(
    val listName: UiText = UiText.DynamicString(""),
    val description: String? = null,
    val iconRes: DrawableResource = null.toDrawableRes(),
    val coverImagePath: String? = null,
    val gameCountText: UiText = UiText.DynamicString(""),
    val isDefaultList: Boolean = false,
    val showListOptions: Boolean = false,
    val showEditAction: Boolean = false,
    val formValues: WishlistFormUiModel = WishlistFormUiModel(),
    val viewMode: WishlistViewMode = WishlistViewMode.LIST,
    val filterChips: List<WishlistFilterChipUiModel> = emptyList(),
    val gamePendingRemoval: GameItemUiModel? = null,
    val contentState: WishlistContentState = WishlistContentState.Loading
)
