package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to resolve the platforms recommendations should be limited to.
 *
 * The user's explicit selection wins whenever they made one; otherwise the platforms carried by
 * their saved games stand in. That fallback is what makes the filter useful before the user has
 * ever opened the picker, and the reason an empty selection means "not set" rather than "own
 * nothing" — the latter would silently empty every feed with no way for the user to tell why.
 */
class GetOwnedPlatformsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * @return A flow of the platforms to filter on, sorted by name, empty only when the user has
     * saved nothing and chosen nothing.
     */
    operator fun invoke(): Flow<List<Platform>> {
        return combine(
            repository.getOwnedPlatformIds(),
            repository.getKnownPlatforms(),
            repository.getInferredPlatforms()
        ) { ownedIds, knownPlatforms, inferredPlatforms ->
            if (ownedIds.isEmpty()) {
                inferredPlatforms
            } else {
                knownPlatforms.filter { it.id in ownedIds }
            }
        }
    }
}
