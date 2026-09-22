package com.example.gameswishlist.core.domain.radar

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import javax.inject.Inject

/** Use case wrapper over [GameRepository.refreshSavedGameReleaseDates], invoked by the periodic refresh worker. */
class RefreshReleaseDatesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return repository.refreshSavedGameReleaseDates()
    }
}
