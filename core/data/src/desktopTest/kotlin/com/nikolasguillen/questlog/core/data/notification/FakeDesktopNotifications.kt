package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationRequest
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotifications
import com.nikolasguillen.questlog.core.common.desktop.DesktopPendingNotification

/** A [DesktopNotifications] that records what it is asked and answers with what the test sets. */
internal class FakeDesktopNotifications : DesktopNotifications {

    var status = DesktopNotificationStatus.AUTHORIZED
    var accepting = true
    var pendingNotifications = emptyList<DesktopPendingNotification>()

    val scheduled = mutableListOf<DesktopNotificationRequest>()
    val removed = mutableListOf<Int>()

    override suspend fun status() = status

    override suspend fun requestAuthorization() = status == DesktopNotificationStatus.AUTHORIZED

    override suspend fun schedule(request: DesktopNotificationRequest): Boolean {
        if (!accepting) return false
        scheduled += request
        return true
    }

    override suspend fun pending() = pendingNotifications

    override suspend fun remove(gameIds: Collection<Int>) {
        removed += gameIds
    }

    override fun setClickHandler(onClick: (gameId: Int) -> Unit) = Unit
}
