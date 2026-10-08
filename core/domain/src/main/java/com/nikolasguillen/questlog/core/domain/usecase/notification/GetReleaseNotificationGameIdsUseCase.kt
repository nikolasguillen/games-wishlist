package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

/** The ids of games with a release reminder enabled, for cheap membership checks against a game list. */
class GetReleaseNotificationGameIdsUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<Set<Int>> = repository.getReleaseNotificationGameIds()
}
