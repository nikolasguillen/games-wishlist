package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve the list of recent search queries.
 *
 * Useful for showing suggestions or past activity in the search interface.
 */
class GetSearchHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow containing the list of recent search queries as strings.
     */
    suspend operator fun invoke(): Flow<List<String>> {
        return repository.getSearchHistory()
    }
}
