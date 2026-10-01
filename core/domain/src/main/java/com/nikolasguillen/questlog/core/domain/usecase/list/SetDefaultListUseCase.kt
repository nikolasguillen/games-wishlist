package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to choose which wishlist is the default.
 */
class SetDefaultListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Makes [listId] the default wishlist. The previous default loses the designation; the games in
     * either list are left alone.
     */
    suspend operator fun invoke(listId: Long) {
        repository.setDefaultList(listId)
    }
}
