package com.nikolasguillen.questlog.core.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class TrimToCapacityTest {

    private val base = Instant.fromEpochSeconds(1_700_000_000)

    private fun pending(id: Int, dayOffset: Int?) = PendingReminder(id, dayOffset?.let { base + it.days })

    @Test
    fun `fewer than the capacity trims nothing`() {
        assertTrue(trimToCapacity((1..3).map { pending(it, it) }, capacity = 5).isEmpty())
    }

    @Test
    fun `exactly the capacity trims nothing`() {
        assertTrue(trimToCapacity((1..5).map { pending(it, it) }, capacity = 5).isEmpty())
    }

    @Test
    fun `70 reminders in shuffled order trim the 6 latest`() {
        val shuffled = (1..70).map { pending(id = it, dayOffset = it) }.shuffled()

        assertEquals((65..70).toSet(), trimToCapacity(shuffled, MAX_PENDING_REMINDERS).toSet())
    }

    @Test
    fun `an immediate trigger sorts first and is kept while later ones are trimmed`() {
        val reminders = listOf(pending(1, 5), pending(2, null), pending(3, 9))

        assertEquals(listOf(3), trimToCapacity(reminders, capacity = 2))
    }

    @Test
    fun `equal dates tie-break by game id`() {
        val reminders = listOf(pending(30, 1), pending(10, 1), pending(20, 1))

        assertEquals(listOf(30), trimToCapacity(reminders, capacity = 2))
    }
}
