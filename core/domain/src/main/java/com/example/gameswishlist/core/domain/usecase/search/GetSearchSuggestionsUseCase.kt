package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.SearchSuggestion
import javax.inject.Inject

/**
 * Use case to fetch search suggestions based on a partial query.
 * Combines local search history and remote game suggestions from IGDB.
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend fun getLocalSuggestions(query: String): List<SearchSuggestion> {
        if (query.isBlank()) return emptyList()
        return repository.getFilteredSearchHistory(query)
            .map { SearchSuggestion.HistorySuggestion(it) }
    }

    suspend fun getRemoteSuggestions(query: String): List<SearchSuggestion> {
        if (query.isBlank()) return emptyList()
        val remoteResult = repository.getRemoteSearchSuggestions(query)
        return if (remoteResult is AppResult.Success) {
            remoteResult.data.map { SearchSuggestion.GameSuggestion(it) }
        } else {
            emptyList()
        }
    }
}
