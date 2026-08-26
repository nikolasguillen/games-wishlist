package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal sealed interface SearchContentState {
    data class Discover(
        val popular: List<GameItemUiModel>,
        val upcoming: List<GameItemUiModel>
    ) : SearchContentState
    data object Loading : SearchContentState
    data object Empty : SearchContentState
    data class Success(
        val games: List<GameItemUiModel>,
        val filters: List<GameFilterUiModel>,
        val allGames: List<Game> = emptyList()
    ) : SearchContentState {
        val activeFilters: List<GameFilterUiModel> = filters.filter { it.selected }
    }
    data class Error(val message: UiText) : SearchContentState
}
