package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import javax.inject.Inject

/**
 * The single entry point behind every "Notify me" bell, so no surface can record an opt-in without also
 * arming (or disarming) its schedule.
 */
class SetReleaseNotificationEnabledUseCase @Inject constructor(
    private val repository: GameRepository,
    private val syncReleaseNotificationsUseCase: SyncReleaseNotificationsUseCase
) {
    suspend operator fun invoke(gameId: Int, enabled: Boolean) {
        repository.setReleaseNotificationEnabled(gameId, enabled)
        syncReleaseNotificationsUseCase(gameId)
    }
}
