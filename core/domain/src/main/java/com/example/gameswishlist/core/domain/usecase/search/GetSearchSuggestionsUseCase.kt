package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to fetch search suggestions based on a partial query.
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        return repository.getFilteredSearchHistory(query)
    }
}
