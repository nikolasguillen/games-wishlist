package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Platform
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to list the platforms the "My platforms" picker can offer.
 *
 * The local `platforms` table is filled from two sides: saving a game writes the platforms it runs on,
 * and [SyncPlatformCatalogUseCase] writes IGDB's catalogue. It is therefore only as complete as the
 * last sync — it is never empty for a user who has saved games, and empty for a new user only until
 * the first sync lands.
 */
class GetKnownPlatformsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<Platform>> {
        return repository.getKnownPlatforms()
    }
}
