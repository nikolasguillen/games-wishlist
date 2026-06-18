package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

class DeleteSearchHistoryItemUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String) {
        repository.deleteSearchHistoryItem(query)
    }
}