package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.data.scheduler.FakeClock
import com.nikolasguillen.questlog.core.data.scheduler.InMemoryPreferencesDataStore
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/**
 * [LocalNotificationReleaseScheduler] has to keep the pending set right without any app code running when a
 * reminder fires: it arms floating 09:00 requests, remembers what it armed in the ledger, and tells a reminder
 * that fired from one that was never armed.
 */
class LocalNotificationReleaseSchedulerTest {

    private val zone = TimeZone.of("Asia/Tokyo")
    private val now = Instant.fromEpochSeconds(1_700_000_000)

    private val center = FakeLocalNotificationCenter(zone)
    private val ledger = ArmedReminderLedger(InMemoryPreferencesDataStore())
    private val clock = FakeClock(now)
    private val text = mockk<ReleaseReminderText>().also {
        coEvery { it.title(any()) } answers { "title ${firstArg<String>()}" }
        coEvery { it.body() } returns "body"
    }

    private val scheduler = LocalNotificationReleaseScheduler(center, text, ledger, clock) { zone }

    private val releaseDay = LocalDateTime(2023, 11, 20, 9, 0)

    private fun reminder(
        gameId: Int = GAME_ID,
        releaseDate: Long = RELEASE_DATE,
        fireAt: Instant = releaseDay.toInstant(zone)
    ) = ReleaseReminder(gameId, "Hades", releaseDate, fireAt)

    @Test
    fun `a future reminder becomes a floating 09 00 request with the text from the port`() = runTest {
        scheduler.schedule(reminder())

        val request = center.added.single()
        assertEquals(GAME_ID, request.gameId)
        assertEquals(releaseDay, request.fireAt)
        assertEquals("title Hades", request.title)
        assertEquals("body", request.body)
    }

    @Test
    fun `a fire time that has passed becomes an immediate request`() = runTest {
        scheduler.schedule(reminder(fireAt = now))

        assertNull(center.added.single().fireAt)
        assertEquals(setOf(GAME_ID), center.pendingIds)
    }

    @Test
    fun `the ledger entry is written only after the centre accepted the request`() = runTest {
        val gate = CompletableDeferred<Unit>().also { center.addGate = it }

        val call = launch { scheduler.schedule(reminder()) }
        advanceUntilIdle()
        assertTrue(ledger.entries().isEmpty())

        gate.complete(Unit)
        call.join()
        assertEquals(mapOf(GAME_ID to RELEASE_DATE), ledger.entries())
    }

    @Test
    fun `a refused request writes no entry and clears an earlier one`() = runTest {
        scheduler.schedule(reminder(releaseDate = 100))
        assertEquals(mapOf(GAME_ID to 100L), ledger.entries())

        center.accepting = false
        scheduler.schedule(reminder(releaseDate = 200))

        assertTrue(ledger.entries().isEmpty())
    }

    @Test
    fun `a request refused before permission is granted is armed by a later schedule`() = runTest {
        center.accepting = false
        scheduler.schedule(reminder())
        assertTrue(center.pendingIds.isEmpty())

        center.accepting = true
        scheduler.schedule(reminder())

        assertEquals(setOf(GAME_ID), center.pendingIds)
        assertEquals(mapOf(GAME_ID to RELEASE_DATE), ledger.entries())
    }

    @Test
    fun `scheduling a date that already fired adds nothing`() = runTest {
        scheduler.schedule(reminder())
        center.fire(GAME_ID)

        scheduler.schedule(reminder())

        assertEquals(1, center.added.size)
        assertTrue(center.pendingIds.isEmpty())
    }

    @Test
    fun `scheduling a different date arms the game again and replaces the entry`() = runTest {
        scheduler.schedule(reminder(releaseDate = 100))
        center.fire(GAME_ID)

        scheduler.schedule(reminder(releaseDate = 200))

        assertEquals(2, center.added.size)
        assertEquals(setOf(GAME_ID), center.pendingIds)
        assertEquals(mapOf(GAME_ID to 200L), ledger.entries())
    }

    @Test
    fun `cancel removes the request and the ledger entry`() = runTest {
        scheduler.schedule(reminder())
        scheduler.schedule(reminder(gameId = OTHER_GAME_ID))

        scheduler.cancel(GAME_ID)

        assertEquals(setOf(OTHER_GAME_ID), center.pendingIds)
        assertEquals(setOf(OTHER_GAME_ID), ledger.entries().keys)
    }

    @Test
    fun `cancel of a game that was never armed is a no-op`() = runTest {
        scheduler.cancel(GAME_ID)

        assertTrue(center.pendingIds.isEmpty())
        assertTrue(ledger.entries().isEmpty())
    }

    @Test
    fun `takeFiredReminders returns the entries with no pending request, once`() = runTest {
        scheduler.schedule(reminder(gameId = GAME_ID, releaseDate = 100))
        scheduler.schedule(reminder(gameId = OTHER_GAME_ID, releaseDate = 200))
        center.fire(GAME_ID)

        assertEquals(mapOf(GAME_ID to 100L), scheduler.takeFiredReminders())
        assertTrue(scheduler.takeFiredReminders().isEmpty())
        assertEquals(mapOf(OTHER_GAME_ID to 200L), ledger.entries())
    }

    @Test
    fun `overlapping schedules for one game apply in call order`() = runTest {
        val gate = CompletableDeferred<Unit>().also { center.addGate = it }

        val first = launch { scheduler.schedule(reminder(releaseDate = 100)) }
        advanceUntilIdle()
        val second = launch { scheduler.schedule(reminder(releaseDate = 200)) }
        advanceUntilIdle()
        gate.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf(GAME_ID, GAME_ID), center.added.map { it.gameId })
        assertEquals(mapOf(GAME_ID to 200L), ledger.entries())
        assertEquals(setOf(GAME_ID), center.pendingIds)
    }

    private companion object {
        const val GAME_ID = 1
        const val OTHER_GAME_ID = 2
        const val RELEASE_DATE = 1_700_400_000L
    }
}
