package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.repository.GameRepository

/**
 * The single entry point behind every "Notify me" bell, so no surface can record an opt-in without also
 * arming (or disarming) its schedule.
 */
class SetReleaseNotificationEnabledUseCase(
    private val repository: GameRepository,
    private val syncReleaseNotificationsUseCase: SyncReleaseNotificationsUseCase
) {
    suspend operator fun invoke(gameId: Int, enabled: Boolean) {
        repository.setReleaseNotificationEnabled(gameId, enabled)
        syncReleaseNotificationsUseCase(gameId)
    }
}
