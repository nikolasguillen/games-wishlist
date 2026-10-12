package com.nikolasguillen.questlog.core.data.scheduler

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Instant

private val LAST_REFRESH_KEY = longPreferencesKey("release_dates_last_refresh_epoch_ms")

/**
 * The rules every iOS release-date refresh obeys, whoever started it: the app at launch or when the saved set
 * changes, or the system's background task.
 *
 * - A refresh runs only if the last *successful* one is missing or at least [RELEASE_DATES_REFRESH_INTERVAL] old,
 *   unless the caller says otherwise.
 * - The time is written only after the use case succeeds, so a failed or cancelled run is tried again.
 * - One refresh runs at a time, in [scope]. A request that arrives during one **joins** it and gets its outcome,
 *   instead of being dropped: a background task that was dropped would report "done" at once and let the system
 *   suspend the app in the middle of the refresh it should have waited for.
 * - A successful refresh is followed by a full release-reminder sync, inside the same run: a date that moved has to
 *   move its reminder, and nothing else runs while the app is in the background. Joiners and the background task
 *   therefore wait for it too.
 */
internal class ReleaseRefreshRunner(
    private val refreshReleaseDates: RefreshReleaseDatesUseCase,
    private val syncReleaseNotifications: SyncReleaseNotificationsUseCase,
    private val settingsDataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val clock: Clock
) {

    private val startLock = Mutex()

    @Volatile
    private var inFlight: Deferred<ReleaseRefreshOutcome>? = null

    /**
     * Refreshes, or joins the refresh already running and returns its outcome; a joiner's [onlyIfDue] is
     * ignored, because the refresh in flight is the one that decides.
     *
     * Throws `CancellationException` if the refresh was cancelled, by [cancelRunning] or because [scope] closed.
     * Cancelling the caller does not cancel the refresh.
     */
    suspend fun run(onlyIfDue: Boolean): ReleaseRefreshOutcome {
        val refresh = startLock.withLock {
            inFlight?.takeIf { it.isActive } ?: scope.async { refresh(onlyIfDue) }.also { inFlight = it }
        }
        return refresh.await()
    }

    /** Cancels the refresh in flight, if any, whoever started it. Nothing is recorded. */
    fun cancelRunning() {
        inFlight?.cancel()
    }

    /**
     * The earliest the system may start the next background run: now if nothing ever succeeded or the last
     * success is overdue, otherwise one interval after it. Counting from the last *success* means asking again at
     * every launch does not push the window out.
     */
    suspend fun nextEarliestBegin(): Instant {
        val now = clock.now()
        val lastRefreshMillis = settingsDataStore.data.first()[LAST_REFRESH_KEY] ?: return now
        return maxOf(now, Instant.fromEpochMilliseconds(lastRefreshMillis) + RELEASE_DATES_REFRESH_INTERVAL)
    }

    private suspend fun refresh(onlyIfDue: Boolean): ReleaseRefreshOutcome {
        if (onlyIfDue && !isDue()) return ReleaseRefreshOutcome.NOT_DUE
        if (refreshReleaseDates() !is AppResult.Success) return ReleaseRefreshOutcome.FAILED
        recordRefresh()
        syncReminders()
        return ReleaseRefreshOutcome.REFRESHED
    }

    // The dates are stored by now, so a failed sync does not make the refresh a failure: the next trigger resyncs.
    private suspend fun syncReminders() {
        try {
            syncReleaseNotifications()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private suspend fun isDue(): Boolean {
        val lastRefreshMillis = settingsDataStore.data.first()[LAST_REFRESH_KEY] ?: return true
        val elapsedMillis = clock.now().toEpochMilliseconds() - lastRefreshMillis
        return elapsedMillis >= RELEASE_DATES_REFRESH_INTERVAL.inWholeMilliseconds
    }

    private suspend fun recordRefresh() {
        settingsDataStore.edit { it[LAST_REFRESH_KEY] = clock.now().toEpochMilliseconds() }
    }
}
