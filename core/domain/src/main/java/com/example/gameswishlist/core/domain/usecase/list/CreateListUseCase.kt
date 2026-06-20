package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
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
     */
    suspend operator fun invoke(name: String, description: String) {
        repository.createList(name, description)
    }
}
