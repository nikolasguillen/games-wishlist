package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game

/**
 * Use case to associate a game with a specific custom list.
 */
class AddGameToListUseCase(
    private val repository: GameRepository
) {
    /**
     * Adds [game] to the list identified by [listId], storing the game first if it is not stored yet.
     *
     * @param game The game to add.
     * @param listId The unique identifier of the target custom list.
     */
    suspend operator fun invoke(game: Game, listId: Long) {
        repository.addGameToList(game, listId)
    }
}
