package com.example.gameswishlist.core.model

/**
 * The outcome of a search: the matching games plus the facets they can be filtered by.
 *
 * @property games The games that matched the query.
 * @property platforms The distinct platforms present in [games], for the filter row.
 * @property genres The distinct genres present in [games], for the filter row.
 */
data class SearchResult(
    val games: List<Game>,
    val platforms: List<Platform>,
    val genres: List<Genre>
)
