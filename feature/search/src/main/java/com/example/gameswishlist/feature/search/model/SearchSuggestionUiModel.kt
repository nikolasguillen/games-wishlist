package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * Composite UI state for search suggestions.
 *
 * @property historySuggestions List of queries from the local search history.
 * @property gameSuggestions List of games suggested by the remote API.
 * @property isLoadingRemote True if a remote search for suggestions is in progress.
 */
@Immutable
data class SearchSuggestionsUiModel(
    val historySuggestions: List<String> = emptyList(),
    val gameSuggestions: List<GameSuggestionUiModel> = emptyList(),
    val isLoadingRemote: Boolean = false
) {
    val isEmpty: Boolean =
        historySuggestions.isEmpty() && gameSuggestions.isEmpty() && !isLoadingRemote
}

/**
 * UI representation of a game suggestion.
 */
@Immutable
data class GameSuggestionUiModel(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val subtitle: String
)
