package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

class UpdateGameUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(game: Game) {
        repository.updateGameDetails(game)
    }
}
