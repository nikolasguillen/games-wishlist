package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.common.desktop.DesktopPendingNotification
import com.nikolasguillen.questlog.core.data.scheduler.InMemoryPreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrphanReminderSweepTest {

    private val notifications = FakeDesktopNotifications()
    private val center = DesktopLocalNotificationCenter(notifications)
    private val ledger = ArmedReminderLedger(InMemoryPreferencesDataStore())

    private fun pending(vararg ids: Int) {
        notifications.pendingNotifications = ids.map { DesktopPendingNotification(it, fireAt = null) }
    }

    @Test
    fun `removes every pending reminder that is not in the ledger`() = runTest {
        pending(1, 2, 3)
        ledger.put(gameId = 2, releaseDateEpochSeconds = 1_800_000_000)

        sweepOrphanReminders(center, ledger)

        assertEquals(listOf(1, 3), notifications.removed)
    }

    @Test
    fun `removes nothing when every pending reminder is in the ledger`() = runTest {
        pending(1, 2)
        ledger.put(1, 1_800_000_000)
        ledger.put(2, 1_800_000_000)

        sweepOrphanReminders(center, ledger)

        assertTrue(notifications.removed.isEmpty())
    }

    @Test
    fun `removes every pending reminder when the ledger is empty`() = runTest {
        pending(4, 5)

        sweepOrphanReminders(center, ledger)

        assertEquals(listOf(4, 5), notifications.removed)
    }

    @Test
    fun `does nothing without pending reminders`() = runTest {
        ledger.put(1, 1_800_000_000)

        sweepOrphanReminders(center, ledger)

        assertTrue(notifications.removed.isEmpty())
    }
}
