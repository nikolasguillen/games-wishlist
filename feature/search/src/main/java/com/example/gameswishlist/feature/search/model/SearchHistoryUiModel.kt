package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

/**
 * UI representation of user's search history activity.
 */
data class SearchHistoryUiModel(
    val queries: List<UiText> = emptyList(),
    val games: List<GameItemUiModel> = emptyList()
) {
    val isEmpty: Boolean get() = queries.isEmpty() && games.isEmpty()
}
