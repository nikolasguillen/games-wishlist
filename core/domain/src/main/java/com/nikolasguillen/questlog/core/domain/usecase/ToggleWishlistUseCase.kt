package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import javax.inject.Inject

/**
 * Use case to toggle the wishlist status of a game.
 */
class ToggleWishlistUseCase @Inject constructor(
    private val repository: GameRepository,
    private val releaseRefreshScheduler: ReleaseRefreshScheduler
) {
    /**
     * Toggles the wishlist status for the provided [game]. Adding it enqueues a one-shot release-date
     * refresh, so a game saved without per-platform dates doesn't wait for the 24h periodic worker;
     * removing it does not.
     *
     * @param game The game model to update.
     */
    suspend operator fun invoke(game: Game) {
        if (repository.toggleWishlist(game)) {
            releaseRefreshScheduler.scheduleImmediateRefresh()
        }
    }
}
