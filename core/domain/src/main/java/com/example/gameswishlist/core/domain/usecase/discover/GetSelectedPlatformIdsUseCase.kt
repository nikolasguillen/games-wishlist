package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the platforms the user picked, as ids.
 *
 * An empty set means no platform filter. Nothing is substituted for it, which is what lets the picker
 * store what the user actually chose and read back the same thing.
 * Use [GetSelectedPlatformsUseCase] when the platforms have to be named rather than matched.
 */
class GetSelectedPlatformIdsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<Set<Int>> {
        return repository.getOwnedPlatformIds()
    }
}
