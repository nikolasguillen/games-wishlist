package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.WishlistDetail
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case to observe a wishlist's details — its metadata, the games it contains, and whether it is the
 * default wishlist.
 *
 * Emits `null` when [listId] no longer exists (e.g. the list was deleted while observed).
 */
class GetWishlistDetailUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(listId: Long): Flow<WishlistDetail?> {
        return combine(
            repository.observeListById(listId),
            repository.getGamesByList(listId),
            repository.observeDefaultListId()
        ) { list, games, defaultListId ->
            list?.let { WishlistDetail(list = it, games = games, isDefault = it.id == defaultListId) }
        }
    }
}
