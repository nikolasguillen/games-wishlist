package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to retrieve all available wishlist lists with an indication of whether
 * a specific game is already assigned to each list.
 */
class GetWishlistAssignmentsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow of [WishlistAssignment] for the given [gameId].
     */
    operator fun invoke(gameId: Int): Flow<List<WishlistAssignment>> {
        return combine(
            repository.getAllLists(),
            repository.getListIdsForGame(gameId)
        ) { lists, assignedListIds ->
            lists.map { list ->
                WishlistAssignment(
                    list = list,
                    isAssigned = list.id in assignedListIds
                )
            }
        }
    }
}
