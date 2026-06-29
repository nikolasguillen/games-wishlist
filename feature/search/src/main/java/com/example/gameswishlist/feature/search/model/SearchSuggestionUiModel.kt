package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * UI representation of a search suggestion.
 */
@Immutable
sealed interface SearchSuggestionUiModel {
    /**
     * Local history suggestion.
     */
    data class History(val query: String) : SearchSuggestionUiModel

    /**
     * Remote game suggestion with pre-formatted display data.
     */
    data class Game(
        val id: Int,
        val name: String,
        val coverUrl: String?,
        val subtitle: String
    ) : SearchSuggestionUiModel
}
