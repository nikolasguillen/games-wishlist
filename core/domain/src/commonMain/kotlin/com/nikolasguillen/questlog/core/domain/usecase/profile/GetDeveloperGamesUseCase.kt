package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Game

/** Use case to load every game a developer is credited with, newest first with undated games last. */
class GetDeveloperGamesUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(developerId: Int): AppResult<List<Game>> {
        return repository.getDevelopedGames(developerId).map { it.newestFirst() }
    }
}
