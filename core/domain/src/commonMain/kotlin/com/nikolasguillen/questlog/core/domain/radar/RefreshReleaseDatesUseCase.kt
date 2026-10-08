package com.nikolasguillen.questlog.core.domain.radar

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult

/** Use case wrapper over [GameRepository.refreshSavedGameReleaseDates], invoked by the periodic refresh worker. */
class RefreshReleaseDatesUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return repository.refreshSavedGameReleaseDates()
    }
}
