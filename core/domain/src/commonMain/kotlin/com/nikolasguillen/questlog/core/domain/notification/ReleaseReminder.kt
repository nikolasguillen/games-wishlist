package com.nikolasguillen.questlog.core.domain.notification

import kotlin.time.Instant

/**
 * The reminder [com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase]
 * hands to a platform [ReleaseNotificationScheduler].
 *
 * [gameName] is text from the data source, so it stays a `String` rather than a `UiText`. [fireAt] is never
 * `null` here: a game that should have no reminder is a `cancel`, not a `schedule`. Android ignores [gameName]
 * and [releaseDateEpochSeconds], because its worker re-reads the game when it fires; iOS needs both, because the
 * system delivers the notification without running any app code.
 */
data class ReleaseReminder(
    val gameId: Int,
    val gameName: String,
    val releaseDateEpochSeconds: Long,
    val fireAt: Instant
)
