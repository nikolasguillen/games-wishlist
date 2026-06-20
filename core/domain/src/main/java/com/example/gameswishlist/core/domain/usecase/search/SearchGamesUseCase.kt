package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.SearchResult
import javax.inject.Inject

/**
 * Use case to search for games using a text query.
 *
 * This use case fetches results from the repository and prepares a [SearchResult]
 * containing the matched games and a curated list of relevant platforms for filtering.
 *
 * It extracts unique platforms from the resulting games and sorts them by their
 * occurrence frequency to provide the most relevant filter chips to the user.
 */
class SearchGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Executes the search for the given [query].
     *
     * @param query The search term entered by the user.
     * @return An [AppResult] containing [SearchResult] with games and derived platform filters.
     */
    suspend operator fun invoke(query: String): AppResult<SearchResult> {
        return repository.searchGames(query).map { games ->
            // Extract all unique platforms present in the search results
            val platforms = games.flatMap { it.platforms }.distinctBy { it.id }
            val genres = games.flatMap { it.genres }.distinctBy { it.id }
            
            // Count occurrences to determine which platforms are most "relevant" for this specific search
            val platformCounts = games.flatMap { it.platforms }.groupingBy { it.id }.eachCount()
            val genreCounts = games.flatMap { it.genres }.groupingBy { it.id }.eachCount()

            SearchResult(
                games = games,
                platforms = platforms
                    .sortedByDescending { platformCounts[it.id] ?: 0 }
                    .map { it },
                genres = genres
                    .sortedByDescending { genreCounts[it.id] ?: 0 }
                    .map { it }
            )
        }
    }
}
