package com.nikolasguillen.questlog.core.data.notification

/**
 * The platform notification centre, as the reminder scheduler sees it. The iOS implementation sits in `iosMain`
 * over `UserNotifications`; the scheduler's tests use a fake.
 */
internal interface LocalNotificationCenter {
    /**
     * `false` when the reminder could not be armed: the app is not authorized to notify (the adapter reads the
     * authorization status first, because the system accepts a request it will never deliver) or the system
     * rejected the request. Never throws except `CancellationException`.
     */
    suspend fun add(request: LocalReminderRequest): Boolean

    suspend fun pendingReminders(): List<PendingReminder>

    suspend fun remove(gameIds: Collection<Int>)
}
