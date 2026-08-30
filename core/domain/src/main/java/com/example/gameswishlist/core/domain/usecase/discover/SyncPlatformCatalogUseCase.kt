package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import javax.inject.Inject

/**
 * Use case to refresh IGDB's platform catalogue into the local cache.
 *
 * Without it the picker can only offer the platforms the user's saved games happen to cover, which is
 * empty for exactly the user the platform filter is meant to help — someone who has saved nothing yet.
 *
 * Callers are expected to fire this and keep rendering from the cache: the result is worth surfacing
 * only when there is nothing cached to fall back on.
 */
class SyncPlatformCatalogUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return repository.syncPlatformCatalog()
    }
}
