package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.ListSelectorItemUiModel

/**
 * Represents the state of the list-selector bottom sheet. If this state is present (non-null) in the
 * UI state, the selector should be displayed.
 *
 * @property gameId The id of the game being added to a list.
 * @property gameName The name of the game being added to a list.
 * @property availableLists The list of all wishlist categories available for the user (working state).
 */
@Immutable
internal data class ListSelectorState(
    val gameId: Int,
    val gameName: String,
    val availableLists: List<ListSelectorItemUiModel> = emptyList()
)
