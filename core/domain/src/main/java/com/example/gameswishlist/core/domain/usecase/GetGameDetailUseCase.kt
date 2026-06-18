package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

class GetGameDetailUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(id: Int): Result<Game> {
        return try {
            val game = repository.getGameDetail(id)
            Result.success(game)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
