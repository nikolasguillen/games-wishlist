package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to clear all games from the recently viewed list.
 */
class ClearRecentGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke() {
        repository.clearRecentGames()
    }
}
