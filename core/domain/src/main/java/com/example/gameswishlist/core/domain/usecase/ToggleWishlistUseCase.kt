package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to toggle the wishlist status of a game.
 */
class ToggleWishlistUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Toggles the wishlist status for the provided [game].
     *
     * @param game The game model to update.
     */
    suspend operator fun invoke(game: Game) {
        repository.toggleWishlist(game)
    }
}
