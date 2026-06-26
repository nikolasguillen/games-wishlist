package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

/**
 * Use case to retrieve detailed information for a specific game.
 *
 * It first attempts to fetch data from the local database, and if not found,
 * it falls back to the network repository.
 */
class GetGameDetailUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Fetches details for the game identified by [id].
     *
     * @param id The unique identifier of the game.
     * @return An [AppResult] containing the [Game] details or an error.
     */
    suspend operator fun invoke(id: Int): AppResult<Game> {
        return repository.getGameDetail(id)
    }
}
