package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.WishlistIcon
import javax.inject.Inject

/**
 * Use case to create a new custom game list.
 */
class CreateListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Creates a new list with the specified [name] and [description].
     *
     * @param name The display name of the new list.
     * @param description An optional description explaining the purpose of the list.
     * @param icon An optional predefined icon representing the list.
     * @param coverImageUri An optional content URI for a cover image picked from the gallery.
     * @return `true` if [coverImageUri] was provided but failed to be persisted, `false` otherwise.
     */
    suspend operator fun invoke(
        name: String,
        description: String,
        icon: WishlistIcon? = null,
        coverImageUri: String? = null
    ): Boolean {
        return repository.createList(name, description, icon, coverImageUri)
    }
}
