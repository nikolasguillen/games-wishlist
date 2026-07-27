package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.WishlistConstants
import javax.inject.Inject

/**
 * Use case to delete a custom game list.
 */
class DeleteListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Deletes the list identified by [listId], together with its game references and its
     * cover image file.
     *
     * The built-in wishlist is never deleted: the rest of the app reads it by a fixed id,
     * so removing it would leave those queries pointing at nothing.
     *
     * @return `true` if the list was deleted, `false` if [listId] is the built-in wishlist.
     */
    suspend operator fun invoke(listId: Long): Boolean {
        if (listId == WishlistConstants.DEFAULT_WISHLIST_ID) return false
        repository.deleteList(listId)
        return true
    }
}
