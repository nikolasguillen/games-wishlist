package com.nikolasguillen.questlog.core.data.notification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * An in-memory [LocalNotificationCenter]. [fire] removes a pending request the way the system does when it
 * delivers one, [accepting] stands in for the notification permission, and [addGate] holds an `add` open.
 */
internal class FakeLocalNotificationCenter(private val zone: TimeZone = TimeZone.UTC) : LocalNotificationCenter {

    var accepting = true

    /** When set, `add` suspends until it completes, before the request is stored. */
    var addGate: CompletableDeferred<Unit>? = null

    /** Every request the centre accepted, in order. */
    val added = mutableListOf<LocalReminderRequest>()

    private val pending = linkedMapOf<Int, LocalReminderRequest>()

    val pendingIds: Set<Int> get() = pending.keys

    fun pendingRequest(gameId: Int): LocalReminderRequest? = pending[gameId]

    /** The system delivered the reminder: it is no longer pending. */
    fun fire(gameId: Int) {
        pending.remove(gameId)
    }

    override suspend fun add(request: LocalReminderRequest): Boolean {
        addGate?.await()
        if (!accepting) return false
        pending[request.gameId] = request
        added += request
        return true
    }

    override suspend fun pendingReminders(): List<PendingReminder> = pending.values.map {
        PendingReminder(it.gameId, it.fireAt?.toInstant(zone))
    }

    override suspend fun remove(gameIds: Collection<Int>) {
        gameIds.forEach { pending.remove(it) }
    }
}
