package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the ids of the games in the default wishlist.
 *
 * Cheap to check a game list against, without loading the full [com.example.gameswishlist.core.model.Game]
 * for every saved title.
 */
class GetWishlistedGameIdsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<Set<Int>> {
        return repository.getWishlistedGameIds()
    }
}
