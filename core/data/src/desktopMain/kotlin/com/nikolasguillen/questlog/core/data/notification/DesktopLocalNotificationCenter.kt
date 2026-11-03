package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationRequest
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotifications

/**
 * [LocalNotificationCenter] over the operating system's notification centre: Windows scheduled toasts or macOS
 * `UserNotifications`. Both keep a scheduled notification and deliver it with the app closed.
 */
internal class DesktopLocalNotificationCenter(
    private val notifications: DesktopNotifications
) : LocalNotificationCenter {

    override suspend fun add(request: LocalReminderRequest): Boolean {
        // The system accepts a request it will never deliver while notifications are off, so ask first.
        return notifications.status() == DesktopNotificationStatus.AUTHORIZED && notifications.schedule(
            DesktopNotificationRequest(
                gameId = request.gameId,
                title = request.title,
                body = request.body,
                fireAt = request.fireAt
            )
        )
    }

    override suspend fun pendingReminders(): List<PendingReminder> =
        notifications.pending().map { PendingReminder(gameId = it.gameId, nextTriggerAt = it.fireAt) }

    override suspend fun remove(gameIds: Collection<Int>) = notifications.remove(gameIds)
}
