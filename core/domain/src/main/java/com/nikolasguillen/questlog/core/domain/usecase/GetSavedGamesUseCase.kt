package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import kotlinx.coroutines.flow.Flow

/** Every game the user has saved (in any list, or carrying a status or a priority). */
class GetSavedGamesUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<Game>> = repository.getSavedGames()
}
