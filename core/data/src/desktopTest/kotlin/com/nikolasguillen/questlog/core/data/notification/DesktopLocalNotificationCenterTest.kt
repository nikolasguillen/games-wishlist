package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.DesktopPendingNotification
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class DesktopLocalNotificationCenterTest {

    private val notifications = FakeDesktopNotifications()
    private val center = DesktopLocalNotificationCenter(notifications)

    private val request = LocalReminderRequest(
        gameId = 7,
        title = "Hades II",
        body = "Out today",
        fireAt = LocalDateTime(2027, 3, 4, 9, 0)
    )

    @Test
    fun `add schedules the request when notifications are authorized`() = runTest {
        assertTrue(center.add(request))

        val scheduled = notifications.scheduled.single()
        assertEquals(7, scheduled.gameId)
        assertEquals("Hades II", scheduled.title)
        assertEquals("Out today", scheduled.body)
        assertEquals(LocalDateTime(2027, 3, 4, 9, 0), scheduled.fireAt)
    }

    @Test
    fun `add is refused without scheduling when the status is not authorized`() = runTest {
        DesktopNotificationStatus.entries.filter { it != DesktopNotificationStatus.AUTHORIZED }.forEach {
            notifications.status = it

            assertFalse(it.name, center.add(request))
        }
        assertTrue(notifications.scheduled.isEmpty())
    }

    @Test
    fun `add reports a request the system rejected`() = runTest {
        notifications.accepting = false

        assertFalse(center.add(request))
    }

    @Test
    fun `a due reminder without a fire time maps to an immediate request`() = runTest {
        center.add(request.copy(fireAt = null))

        assertNull(notifications.scheduled.single().fireAt)
    }

    @Test
    fun `pendingReminders maps the pending notifications`() = runTest {
        val at = Instant.fromEpochSeconds(1_800_000_000)
        notifications.pendingNotifications = listOf(
            DesktopPendingNotification(gameId = 1, fireAt = at),
            DesktopPendingNotification(gameId = 2, fireAt = null)
        )

        assertEquals(listOf(PendingReminder(1, at), PendingReminder(2, null)), center.pendingReminders())
    }

    @Test
    fun `remove forwards the ids`() = runTest {
        center.remove(listOf(3, 4))

        assertEquals(listOf(3, 4), notifications.removed)
    }
}
