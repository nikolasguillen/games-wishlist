package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to record which platforms the user owns.
 *
 * Replaces the whole selection rather than adding to it, so the caller passes the full set the
 * picker is showing.
 */
class SetOwnedPlatformsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * @param platformIds The platforms the user owns. Passing an empty set clears the override and
     * hands the decision back to [GetOwnedPlatformsUseCase]'s inferred default.
     */
    suspend operator fun invoke(platformIds: Set<Int>) {
        repository.setOwnedPlatforms(platformIds)
    }
}
