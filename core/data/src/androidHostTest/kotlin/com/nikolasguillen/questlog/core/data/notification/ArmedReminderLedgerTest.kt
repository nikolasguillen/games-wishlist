package com.nikolasguillen.questlog.core.data.notification

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nikolasguillen.questlog.core.data.scheduler.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmedReminderLedgerTest {

    private val key = stringSetPreferencesKey("release_reminders_armed")
    private val dataStore = InMemoryPreferencesDataStore()
    private val ledger = ArmedReminderLedger(dataStore)

    @Test
    fun `put stores the game and its release date under the ledger key`() = runTest {
        ledger.put(gameId = 7, releaseDateEpochSeconds = 1_700_000_000)

        assertEquals(setOf("7:1700000000"), dataStore.data.first()[key])
        assertEquals(mapOf(7 to 1_700_000_000L), ledger.entries())
    }

    @Test
    fun `a second put for the same game replaces its date`() = runTest {
        ledger.put(7, 100)
        ledger.put(8, 200)
        ledger.put(7, 300)

        assertEquals(mapOf(7 to 300L, 8 to 200L), ledger.entries())
        assertEquals(2, dataStore.data.first()[key]?.size)
    }

    @Test
    fun `remove drops only the given ids`() = runTest {
        ledger.put(1, 100)
        ledger.put(2, 200)
        ledger.put(3, 300)

        ledger.remove(listOf(1, 3))

        assertEquals(mapOf(2 to 200L), ledger.entries())
    }

    @Test
    fun `an empty ledger has no entries`() = runTest {
        assertTrue(ledger.entries().isEmpty())
    }

    @Test
    fun `a malformed stored entry is ignored instead of thrown`() = runTest {
        dataStore.edit { it[key] = setOf("garbage", "3:", ":4", "x:5", "6:y", "9:900") }

        assertEquals(mapOf(9 to 900L), ledger.entries())
    }
}
