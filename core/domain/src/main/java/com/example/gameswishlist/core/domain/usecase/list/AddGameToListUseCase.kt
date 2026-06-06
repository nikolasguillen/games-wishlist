package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.data.repository.GameRepository
import javax.inject.Inject

class AddGameToListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(gameId: Int, listId: Long) {
        repository.addGameToList(gameId, listId)
    }
}
