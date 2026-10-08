package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import kotlin.time.Instant

/**
 * [ReleaseNotificationScheduler] for a platform without release reminders. Nothing reaches it from the UI,
 * because `ReleaseRemindersAvailability` hides every entry point; it is bound only so that the shared use cases
 * that take a scheduler can still be built.
 */
class NoOpReleaseNotificationScheduler : ReleaseNotificationScheduler {
    override fun schedule(gameId: Int, at: Instant) = Unit

    override fun cancel(gameId: Int) = Unit
}
