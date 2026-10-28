package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder

/**
 * Placeholder that arms nothing, so the desktop Koin graph resolves while the reminders are built
 * (specs/020-desktop-app, T019). It is deleted when `LocalNotificationReleaseScheduler` is bound in its place (T062).
 */
internal class InertReleaseNotificationScheduler : ReleaseNotificationScheduler {

    override suspend fun schedule(reminder: ReleaseReminder) = Unit

    override suspend fun cancel(gameId: Int) = Unit

    override suspend fun takeFiredReminders(): Map<Int, Long> = emptyMap()
}
