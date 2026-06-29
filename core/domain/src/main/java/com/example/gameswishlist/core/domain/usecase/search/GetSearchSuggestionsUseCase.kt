package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.SearchSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    fun getRemoteSuggestionsFlow(query: String): Flow<List<SearchSuggestion>> = flow {
        if (query.isNotBlank()) {
            val remoteResult = repository.getRemoteSearchSuggestions(query)
            if (remoteResult is AppResult.Success) {
                emit(remoteResult.data.map { SearchSuggestion.GameSuggestion(it) })
            } else {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }
}
