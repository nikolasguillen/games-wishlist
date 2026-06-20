package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to add a query string to the local search history.
 */
class AddSearchToHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Persists the [query] into the search history database.
     *
     * @param query The search term to save.
     */
    suspend operator fun invoke(query: String) {
        repository.addSearchToHistory(query)
    }
}
