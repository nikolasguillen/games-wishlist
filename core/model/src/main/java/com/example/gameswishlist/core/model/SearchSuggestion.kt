package com.example.gameswishlist.core.model

/**
 * Represents a suggestion in the search dropdown.
 */
sealed interface SearchSuggestion {
    /**
     * A suggestion from the local search history.
     *
     * @property query The historical search term.
     */
    data class HistorySuggestion(val query: String) : SearchSuggestion

    /**
     * A suggestion for a specific game from the remote API.
     *
     * @property game The suggested game details.
     */
    data class GameSuggestion(val game: Game) : SearchSuggestion
}
