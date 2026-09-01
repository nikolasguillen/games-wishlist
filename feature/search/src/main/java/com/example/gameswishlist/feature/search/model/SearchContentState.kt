package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

/**
 * Lifecycle of the search results.
 *
 * [Idle] is the resting state and carries the mode of the whole screen with it: no search is active,
 * so the Discover feed owns the content area and [SearchUiState.discover] is what gets rendered.
 * Every other case means a search has been committed and holds the screen until it is cleared.
 */
@Immutable
internal sealed interface SearchContentState {
    /** No committed search. The Discover feed is on screen. */
    data object Idle : SearchContentState
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
