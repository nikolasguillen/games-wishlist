package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game

/**
 * Use case to update an existing game's details in the local storage.
 *
 * This includes personal user data such as notes, priority, and wishlist status.
 */
class UpdateGameUseCase(
    private val repository: GameRepository
) {
    /**
     * Updates the provided [game] details.
     *
     * @param game The game model with updated information.
     */
    suspend operator fun invoke(game: Game) {
        repository.updateGameDetails(game)
    }
}
