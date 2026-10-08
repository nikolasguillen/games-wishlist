package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.repository.GameRepository

/**
 * Use case to delete a custom game list.
 */
class DeleteListUseCase(
    private val repository: GameRepository
) {
    /**
     * Deletes the list identified by [listId], together with its game references and its
     * cover image file.
     *
     * The current default wishlist is never deleted: the heart button and every saved-game mark resolve
     * to it, so removing it would leave them pointing at nothing. The database refuses that delete too.
     *
     * @return `true` if the list was deleted, `false` if [listId] is the default wishlist.
     */
    suspend operator fun invoke(listId: Long): Boolean {
        if (listId == repository.getDefaultListId()) return false
        repository.deleteList(listId)
        return true
    }
}
