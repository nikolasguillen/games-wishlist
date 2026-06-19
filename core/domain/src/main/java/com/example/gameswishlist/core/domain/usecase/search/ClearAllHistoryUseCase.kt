package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

class ClearAllHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke() {
        repository.clearSearchHistory()
    }
}