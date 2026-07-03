package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to remove a game from a specific custom list.
 */
class RemoveGameFromListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Removes the game identified by [gameId] from the list identified by [listId].
     *
     * @param gameId The unique identifier of the game to remove.
     * @param listId The unique identifier of the target custom list.
     */
    suspend operator fun invoke(gameId: Int, listId: Long) {
        repository.removeGameFromList(gameId, listId)
    }
}
