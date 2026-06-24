package com.example.gameswishlist.core.model

/**
 * Composite model representing the user's recent activity in the search context.
 *
 * @property queries List of terms previously searched by the user.
 * @property games List of games recently viewed by the user.
 */
data class RecentSearchActivity(
    val queries: List<String> = emptyList(),
    val games: List<Game> = emptyList()
)
