package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.WishlistSummary
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case to retrieve all custom user-defined game lists, each with whether it is the default.
 *
 * Provides a continuous stream of lists stored in the local database. It re-emits when the default
 * wishlist changes, so the overview is already correct when the user comes back from changing it.
 */
class GetListsUseCase(
    private val repository: GameRepository
) {
    /**
     * Returns a flow containing the current lists as [WishlistSummary], in the order the repository
     * emits them.
     */
    operator fun invoke(): Flow<List<WishlistSummary>> {
        return combine(
            repository.getAllLists(),
            repository.observeDefaultListId()
        ) { lists, defaultListId ->
            lists.map { WishlistSummary(list = it, isDefault = it.id == defaultListId) }
        }
    }
}
