package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository

/**
 * Use case to remove a specific game from the recently viewed list.
 * Note: This does not delete the game from the cache, just clears its "last viewed" status.
 */
class RemoveRecentGameUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(gameId: Int) {
        repository.removeRecentGame(gameId)
    }
}
