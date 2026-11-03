package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Keeps the pending reminders matching the app's state on iOS and desktop, where the system fires them with no app
 * code running: [beforeFirstSync] once, then one full sync at launch, one for every [ReminderSyncTriggers] event, and a sync of a single game when it
 * leaves the saved set (so its reminder disappears at once instead of at the next launch).
 *
 * [beforeFirstSync] is for work that must finish before the launch sync reads the notification centre (desktop sweeps
 * the reminders a reinstall left behind). A failure in it is swallowed and the sync still runs. iOS passes nothing.
 *
 * Public with an `internal` constructor: `startQuestLog` in `:shared` calls [start], and Koin builds it here.
 */
class ReleaseReminderCoordinator internal constructor(
    private val syncReleaseNotifications: SyncReleaseNotificationsUseCase,
    private val getSavedGameIds: () -> Flow<Set<Int>>,
    private val triggers: ReminderSyncTriggers,
    private val scope: CoroutineScope,
    private val beforeFirstSync: suspend () -> Unit = {}
) {

    // Taken once and never released: tryLock is the atomic "first caller wins" that start() needs.
    private val startGuard = Mutex()

    /** Idempotent. */
    fun start() {
        if (!startGuard.tryLock()) return

        // Conflated: full syncs never overlap, and a trigger that arrives during one is neither dropped nor queued
        // many times over, it becomes exactly one follow-up sync.
        val fullSyncRequests = Channel<Unit>(Channel.CONFLATED)
        fullSyncRequests.trySend(Unit)

        scope.launch {
            runBeforeFirstSync()
            for (request in fullSyncRequests) sync(gameId = null)
        }
        scope.launch { triggers.events.collect { fullSyncRequests.trySend(Unit) } }
        scope.launch { syncGamesThatLeaveTheSavedSet() }
    }

    private suspend fun syncGamesThatLeaveTheSavedSet() {
        // The first emission is the baseline: a game that was never saved this session did not just leave.
        var previous: Set<Int>? = null
        getSavedGameIds().collect { current ->
            previous?.minus(current)?.forEach { sync(gameId = it) }
            previous = current
        }
    }

    private suspend fun runBeforeFirstSync() {
        try {
            beforeFirstSync()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The sweep only removes leftovers: without it the launch sync still brings the reminders up to date.
        }
    }

    private suspend fun sync(gameId: Int?) {
        try {
            syncReleaseNotifications(gameId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A failed sync is retried by the next trigger, so it must not end the coordinator.
        }
    }
}
