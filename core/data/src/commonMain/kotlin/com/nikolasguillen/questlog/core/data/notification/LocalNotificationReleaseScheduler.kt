package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * [ReleaseNotificationScheduler] over a [LocalNotificationCenter], for a platform whose system delivers the
 * notification without running any app code (iOS). Because nothing runs at fire time, the pending set has to be
 * right ahead of time, and a reminder that fired can only be recognised afterwards by the [ArmedReminderLedger].
 *
 * Every call runs under one lock, so calls for the same game apply in the order they were made.
 */
internal class LocalNotificationReleaseScheduler(
    private val center: LocalNotificationCenter,
    private val text: ReleaseReminderText,
    private val ledger: ArmedReminderLedger,
    private val clock: Clock,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : ReleaseNotificationScheduler {

    private val mutex = Mutex()

    override suspend fun schedule(reminder: ReleaseReminder) = mutex.withLock {
        val gameId = reminder.gameId

        // The ledger has this very date but the request is gone: it fired. Arming it again would notify twice.
        val alreadyFired = ledger.entries()[gameId] == reminder.releaseDateEpochSeconds &&
            center.pendingReminders().none { it.gameId == gameId }
        if (alreadyFired) return@withLock

        val fireAt = reminder.fireAt.takeIf { it > clock.now() }?.toLocalDateTime(timeZone())
        val accepted = center.add(
            LocalReminderRequest(
                gameId = gameId,
                title = text.title(reminder.gameName),
                body = text.body(),
                fireAt = fireAt
            )
        )
        if (!accepted) {
            // Not armed, so nothing may claim it was: the next sync after permission is granted arms it.
            ledger.remove(listOf(gameId))
            return@withLock
        }
        ledger.put(gameId, reminder.releaseDateEpochSeconds)
    }

    override suspend fun cancel(gameId: Int) = mutex.withLock {
        center.remove(listOf(gameId))
        ledger.remove(listOf(gameId))
    }

    override suspend fun takeFiredReminders(): Map<Int, Long> = mutex.withLock {
        val armed = ledger.entries()
        if (armed.isEmpty()) return@withLock emptyMap()

        val pendingIds = center.pendingReminders().mapTo(mutableSetOf()) { it.gameId }
        val fired = armed.filterKeys { it !in pendingIds }
        ledger.remove(fired.keys)
        fired
    }
}
