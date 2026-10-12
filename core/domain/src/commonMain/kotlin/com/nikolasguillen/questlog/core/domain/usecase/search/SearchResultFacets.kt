package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.SearchResult

/**
 * Wraps these games in a [SearchResult] together with the facets the filter sheet offers for them: the
 * distinct platforms and genres they carry, each ordered by how many of the games carry it, so the most
 * relevant filter chips for this particular result come first. Ties keep the order of first appearance.
 * The games keep their order.
 *
 * @param excludedGenreId A genre left out of the genre facet. A genre browse passes the browsed genre: every
 * game carries it, so offering it as a filter would narrow nothing.
 */
internal fun List<Game>.toSearchResult(excludedGenreId: Int? = null): SearchResult {
    val platformCounts = flatMap { it.platforms }.groupingBy { it.id }.eachCount()
    val genreCounts = flatMap { it.genres }.groupingBy { it.id }.eachCount()

    return SearchResult(
        games = this,
        platforms = flatMap { it.platforms }
            .distinctBy { it.id }
            .sortedByDescending { platformCounts[it.id] ?: 0 },
        genres = flatMap { it.genres }
            .distinctBy { it.id }
            .filter { it.id != excludedGenreId }
            .sortedByDescending { genreCounts[it.id] ?: 0 }
    )
}
