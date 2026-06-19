package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.collections.map

class GetSearchHistoryUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): Flow<List<String>> {
        return repository.getSearchHistory()
    }
}