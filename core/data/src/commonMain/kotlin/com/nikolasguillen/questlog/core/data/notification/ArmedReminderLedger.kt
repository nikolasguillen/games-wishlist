package com.nikolasguillen.questlog.core.data.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

private val ARMED_KEY = stringSetPreferencesKey("release_reminders_armed")

/**
 * Which reminder, for which release date, the notification centre was last given for each game. The system fires
 * a local notification without running any app code, so this is the only way to tell later that a reminder fired:
 * an entry whose request is no longer pending was delivered.
 *
 * Stored as `"<gameId>:<releaseDateEpochSeconds>"`, at most one entry per game. Only the iOS reminder scheduler
 * reads or writes it.
 */
internal class ArmedReminderLedger(private val dataStore: DataStore<Preferences>) {

    suspend fun entries(): Map<Int, Long> = dataStore.data.first()[ARMED_KEY].orEmpty().parsed().toMap()

    /** Records [releaseDateEpochSeconds] for [gameId], replacing any earlier date for that game. */
    suspend fun put(gameId: Int, releaseDateEpochSeconds: Long) {
        dataStore.edit { preferences ->
            val others = preferences[ARMED_KEY].orEmpty().parsed().filter { (id, _) -> id != gameId }
            preferences[ARMED_KEY] = (others + (gameId to releaseDateEpochSeconds)).map { (id, date) -> "$id:$date" }.toSet()
        }
    }

    suspend fun remove(gameIds: Collection<Int>) {
        if (gameIds.isEmpty()) return
        dataStore.edit { preferences ->
            val remaining = preferences[ARMED_KEY].orEmpty().parsed().filter { (id, _) -> id !in gameIds }
            preferences[ARMED_KEY] = remaining.map { (id, date) -> "$id:$date" }.toSet()
        }
    }

    // A malformed entry is skipped rather than thrown, so one bad string cannot stop every reminder from syncing.
    private fun Set<String>.parsed(): List<Pair<Int, Long>> = mapNotNull { entry ->
        val parts = entry.split(':')
        val id = parts.getOrNull(0)?.toIntOrNull()
        val date = parts.getOrNull(1)?.toLongOrNull()
        if (parts.size == 2 && id != null && date != null) id to date else null
    }
}
