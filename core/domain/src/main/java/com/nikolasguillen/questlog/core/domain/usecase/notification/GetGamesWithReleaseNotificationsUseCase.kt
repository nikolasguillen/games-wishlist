package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The saved games with a release reminder enabled, for the Settings management sub-screen. */
class GetGamesWithReleaseNotificationsUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<Game>> = repository.getGamesWithReleaseNotifications()
}
