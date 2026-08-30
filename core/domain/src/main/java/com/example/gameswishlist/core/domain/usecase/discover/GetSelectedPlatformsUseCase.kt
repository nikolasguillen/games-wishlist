package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to resolve the user's platform selection into full [Platform]s, for callers that need to
 * name them rather than match on them.
 *
 * An empty result means the user picked nothing, which means **no platform filter** — it is not a
 * missing value to be guessed at. An earlier design substituted the platforms carried by the user's
 * saved games here; that was dropped because it filtered the feed on a rule the user could neither see
 * nor explain, and because it did nothing at all for the brand-new user it was supposed to help.
 * Use [GetSelectedPlatformIdsUseCase] when the ids are enough.
 */
class GetSelectedPlatformsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * @return A flow of the selected platforms, sorted by name. Ids that no longer match a cached
     * platform are dropped rather than surfaced as gaps.
     */
    operator fun invoke(): Flow<List<Platform>> {
        return combine(
            repository.getOwnedPlatformIds(),
            repository.getKnownPlatforms()
        ) { selectedIds, knownPlatforms ->
            knownPlatforms.filter { it.id in selectedIds }
        }
    }
}
