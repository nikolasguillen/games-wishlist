package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Platform
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to list the platforms the "My platforms" picker can offer.
 *
 * The local `platforms` table is filled as a side effect of saving games, so this grows with use and
 * is not the full IGDB catalogue — a user who saved nothing has nothing to pick from.
 */
class GetKnownPlatformsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<Platform>> {
        return repository.getKnownPlatforms()
    }
}
