package com.example.gameswishlist.core.domain.usecase

import com.example.gameswishlist.core.data.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import javax.inject.Inject

class SearchGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<Game>> {
        return repository.searchGames(query)
    }
}
