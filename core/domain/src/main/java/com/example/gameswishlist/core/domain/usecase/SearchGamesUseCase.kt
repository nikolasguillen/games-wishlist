package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.data.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

class SearchGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): Result<List<Game>> {
        return try {
            val games = repository.searchGames(query)
            Result.success(games)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
