package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.data.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGamesByListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(listId: Long): Flow<List<Game>> {
        return repository.getGamesByList(listId)
    }
}
