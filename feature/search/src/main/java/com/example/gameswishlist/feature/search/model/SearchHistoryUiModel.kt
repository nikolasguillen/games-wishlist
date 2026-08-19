package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.GameItemUiModel

/**
 * UI representation of user's search history activity.
 */
@Immutable
internal data class SearchHistoryUiModel(
    val queries: List<String> = emptyList(),
    val games: List<GameItemUiModel> = emptyList()
) {
    val isEmpty: Boolean get() = queries.isEmpty() && games.isEmpty()
}
