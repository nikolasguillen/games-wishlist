package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to clear the entire local search history.
 */
class ClearAllHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Deletes all records from the search history database.
     */
    suspend operator fun invoke() {
        repository.clearSearchHistory()
    }
}
