package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

/**
 * Use case to fetch search suggestions based on a partial query.
 */
class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<Game>> {
        if (query.length < 3) return AppResult.success(emptyList())
        return repository.getSearchSuggestions(query)
    }
}
