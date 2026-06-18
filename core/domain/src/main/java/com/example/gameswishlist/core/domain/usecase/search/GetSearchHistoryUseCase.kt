package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.SearchHistoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSearchHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): Flow<List<SearchHistoryItem>> {
        return repository.getSearchHistory()
    }
}