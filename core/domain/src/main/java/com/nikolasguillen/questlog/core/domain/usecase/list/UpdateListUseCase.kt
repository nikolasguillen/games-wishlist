package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.model.CoverImageUpdate
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.WishlistIcon
import javax.inject.Inject

/**
 * Use case to edit the details of an existing custom game list.
 */
class UpdateListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Saves [name], [description], [icon] and the [coverImage] change on the list identified by [listId].
     * The list's games, their statuses and whether it is the default wishlist are left alone.
     *
     * @return The list itself is always updated; a [AppResult.Failure] only indicates that [coverImage]
     * was a [CoverImageUpdate.Replace] whose image failed to be persisted, in which case the previous
     * cover is kept.
     */
    suspend operator fun invoke(
        listId: Long,
        name: String,
        description: String,
        icon: WishlistIcon?,
        coverImage: CoverImageUpdate
    ): AppResult<Unit> {
        return repository.updateList(listId, name, description, icon, coverImage)
    }
}
