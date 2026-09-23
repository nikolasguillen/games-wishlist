package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe detailed information for a specific game, reactively.
 *
 * Reads straight from local storage; emits `null` until the game has been cached at
 * least once. Pair with [RefreshGameDetailUseCase] to hydrate the cache from the network.
 */
class GetGameDetailUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(id: Int): Flow<Game?> {
        return repository.observeGameDetail(id)
    }
}
