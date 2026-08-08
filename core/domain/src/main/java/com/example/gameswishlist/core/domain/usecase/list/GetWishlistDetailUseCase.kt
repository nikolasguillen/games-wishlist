package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.model.WishlistDetail
import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to observe a wishlist's details — its metadata and the games it contains.
 *
 * Emits `null` when [listId] no longer exists (e.g. the list was deleted while observed).
 */
class GetWishlistDetailUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(listId: Long): Flow<WishlistDetail?> {
        return combine(
            repository.observeListById(listId),
            repository.getGamesByList(listId)
        ) { list, games ->
            list?.let { WishlistDetail(list = it, games = games) }
        }
    }
}
