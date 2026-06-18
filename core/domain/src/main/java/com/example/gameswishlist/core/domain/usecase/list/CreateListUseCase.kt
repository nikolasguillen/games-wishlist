package com.example.gameswishlist.core.domain.usecase.list

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

class CreateListUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(name: String, description: String) {
        repository.createList(name, description)
    }
}
