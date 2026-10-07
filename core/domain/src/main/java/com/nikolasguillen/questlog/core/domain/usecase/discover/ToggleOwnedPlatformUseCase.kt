package com.nikolasguillen.questlog.core.domain.usecase.discover

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * Use case to flip one platform in the user's owned set: owned becomes not owned and the other way round.
 *
 * Deliberately a single-platform write rather than "replace the whole set": the read-modify-write happens
 * inside one transaction in the data layer, so two taps in quick succession — from any screen — can never
 * overwrite each other. Emptying the set turns the platform filter off.
 */
class ToggleOwnedPlatformUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(platformId: Int) {
        repository.toggleOwnedPlatform(platformId)
    }
}
