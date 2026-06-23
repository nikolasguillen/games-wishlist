package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve the list of games recently viewed by the user.
 */
class GetRecentlyViewedGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow containing the list of recently viewed games.
     */
    operator fun invoke(): Flow<List<Game>> {
        return repository.getRecentlyViewedGames()
    }
}
