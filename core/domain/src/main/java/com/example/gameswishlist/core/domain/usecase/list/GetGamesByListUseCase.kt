package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve all games belonging to a specific custom list.
 *
 * Provides a continuous stream of games associated with the given list ID.
 */
class GetGamesByListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow containing the list of [Game] entities for the specified [listId].
     *
     * @param listId The unique identifier of the custom list.
     */
    operator fun invoke(listId: Long): Flow<List<Game>> {
        return repository.getGamesByList(listId)
    }
}
