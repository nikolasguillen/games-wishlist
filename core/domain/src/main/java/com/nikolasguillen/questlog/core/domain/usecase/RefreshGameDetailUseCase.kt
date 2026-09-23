package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import javax.inject.Inject

/**
 * Use case to hydrate a game's local cache: read local storage first, fall back to the
 * network if absent, then persist the result so [GetGameDetailUseCase]'s observation picks
 * it up.
 */
class RefreshGameDetailUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(id: Int): AppResult<Unit> {
        return repository.refreshGameDetail(id)
    }
}
