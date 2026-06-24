package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to remove a specific game from the recently viewed list.
 * Note: This does not delete the game from the cache, just clears its "last viewed" status.
 */
class RemoveRecentGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(gameId: Int) {
        repository.removeRecentGame(gameId)
    }
}
