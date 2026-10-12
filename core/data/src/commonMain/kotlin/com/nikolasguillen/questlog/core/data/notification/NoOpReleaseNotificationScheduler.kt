package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder

/**
 * [ReleaseNotificationScheduler] for a platform without release reminders. Nothing reaches it from the UI,
 * because `ReleaseRemindersAvailability` hides every entry point; it is bound only so that the shared use cases
 * that take a scheduler can still be built.
 */
internal class NoOpReleaseNotificationScheduler : ReleaseNotificationScheduler {
    override suspend fun schedule(reminder: ReleaseReminder) = Unit

    override suspend fun cancel(gameId: Int) = Unit

    override suspend fun takeFiredReminders(): Map<Int, Long> = emptyMap()
}
