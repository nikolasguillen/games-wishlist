package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.SearchResult

/**
 * Use case to search for games using a text query.
 *
 * This use case fetches results from the repository and prepares a [SearchResult]
 * containing the matched games and a curated list of relevant platforms and genres for filtering.
 *
 * The facets are the unique platforms and genres of the resulting games, sorted by their occurrence
 * frequency to provide the most relevant filter chips to the user (see [toSearchResult]).
 */
class SearchGamesUseCase(
    private val repository: GameRepository
) {
    /**
     * Executes the search for the given [query].
     *
     * @param query The search term entered by the user.
     * @return An [AppResult] containing [SearchResult] with games and derived platform and genre filters.
     */
    suspend operator fun invoke(query: String): AppResult<SearchResult> {
        return repository.searchGames(query).map { it.toSearchResult() }
    }
}
