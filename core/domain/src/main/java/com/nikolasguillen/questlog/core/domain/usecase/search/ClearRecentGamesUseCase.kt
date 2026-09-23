package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to clear all games from the recently viewed list.
 */
class ClearRecentGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke() {
        repository.clearRecentGames()
    }
}
