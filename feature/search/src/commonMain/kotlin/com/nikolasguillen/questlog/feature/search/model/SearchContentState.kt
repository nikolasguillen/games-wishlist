package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

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

    /**
     * A Characters or Developers search that found something; which of the two is [SearchUiState.resultsType].
     * Never empty: a lookup that found nothing is [Empty].
     */
    data class EntityResults(val items: List<EntityResultUiModel>) : SearchContentState
    data class Error(val message: UiText) : SearchContentState
}
