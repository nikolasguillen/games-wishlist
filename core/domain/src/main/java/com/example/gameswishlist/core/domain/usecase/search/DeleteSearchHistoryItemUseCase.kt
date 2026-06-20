package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to remove a specific item from the local search history.
 */
class DeleteSearchHistoryItemUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Deletes the specified [query] from the search history database.
     *
     * @param query The search term to remove.
     */
    suspend operator fun invoke(query: String) {
        repository.deleteSearchHistoryItem(query)
    }
}
