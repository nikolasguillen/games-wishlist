package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

/**
 * Use case to update an existing game's details in the local storage.
 *
 * This includes personal user data such as notes, priority, and wishlist status.
 */
class UpdateGameUseCase @Inject constructor(
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
