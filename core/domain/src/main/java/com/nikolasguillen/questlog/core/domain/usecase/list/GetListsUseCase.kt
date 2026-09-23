package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.WishlistList
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve all custom user-defined game lists.
 *
 * Provides a continuous stream of lists stored in the local database.
 */
class GetListsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow containing the current list of all [WishlistList] entities.
     */
    operator fun invoke(): Flow<List<WishlistList>> {
        return repository.getAllLists()
    }
}
