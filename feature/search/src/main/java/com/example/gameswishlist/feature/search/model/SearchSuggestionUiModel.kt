package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.ui.model.UiText

/**
 * Represents a single item in the search suggestions list.
 * Can be either a game from the API or a previous search query from history.
 */
sealed interface SearchSuggestionUiModel {
    val id: String
    val text: UiText

    /**
     * A suggestion representing a specific game.
     */
    data class Game(
        val gameId: Int,
        override val text: UiText,
        val coverUrl: String? = null,
        val developer: UiText? = null,
        val releaseYear: UiText? = null
    ) : SearchSuggestionUiModel {
        override val id: String = "game_$gameId"
    }

    /**
     * A suggestion representing a past search query.
     */
    data class RecentSearch(
        override val text: UiText
    ) : SearchSuggestionUiModel {
        override val id: String = "recent_$text"
    }
}
