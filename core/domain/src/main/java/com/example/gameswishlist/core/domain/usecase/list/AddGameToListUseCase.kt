package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to associate a game with a specific custom list.
 */
class AddGameToListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Adds the game identified by [gameId] to the list identified by [listId].
     *
     * @param gameId The unique identifier of the game to add.
     * @param listId The unique identifier of the target custom list.
     */
    suspend operator fun invoke(gameId: Int, listId: Long) {
        repository.addGameToList(gameId, listId)
    }
}
