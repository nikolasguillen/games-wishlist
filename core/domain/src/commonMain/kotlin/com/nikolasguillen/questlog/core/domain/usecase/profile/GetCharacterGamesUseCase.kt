package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Game

/**
 * Use case to load the games a character appears in, from the ids on its profile, newest first with undated
 * games last. An empty id list is an empty success and makes no request.
 */
class GetCharacterGamesUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(gameIds: List<Int>): AppResult<List<Game>> {
        if (gameIds.isEmpty()) return AppResult.success(emptyList())
        return repository.getGamesByIds(gameIds).map { it.newestFirst() }
    }
}
