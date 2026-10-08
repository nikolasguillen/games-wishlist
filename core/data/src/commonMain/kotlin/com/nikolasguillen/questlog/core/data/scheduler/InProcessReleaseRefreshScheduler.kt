package com.nikolasguillen.questlog.core.data.scheduler

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

private val LAST_REFRESH_KEY = longPreferencesKey("release_dates_last_refresh_epoch_ms")

/**
 * [ReleaseRefreshScheduler] for a platform with no background job runner: the refresh runs inside the app,
 * in [scope], at launch and whenever the saved set changes. While the app is closed nothing refreshes; the
 * next launch catches up.
 *
 * - [schedulePeriodicRefresh] runs only if the last *successful* refresh is missing or at least
 *   [RELEASE_DATES_REFRESH_INTERVAL] old.
 * - [scheduleImmediateRefresh] runs now, whatever the timestamp.
 * - Only one refresh runs at a time; a request that arrives during one is dropped, as WorkManager's `KEEP`
 *   policy does on Android.
 * - The timestamp is written only after a refresh succeeds, so a failed or interrupted run is tried again at
 *   the next launch.
 */
class InProcessReleaseRefreshScheduler(
    private val refreshReleaseDates: RefreshReleaseDatesUseCase,
    private val settingsDataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
    private val clock: Clock
) : ReleaseRefreshScheduler {

    private val running = Mutex()

    override fun schedulePeriodicRefresh() = launchRefresh(onlyIfDue = true)

    override fun scheduleImmediateRefresh() = launchRefresh(onlyIfDue = false)

    private fun launchRefresh(onlyIfDue: Boolean) {
        // Taken here, before the launch, so two requests in a row cannot both slip past the check.
        if (!running.tryLock()) return
        scope.launch {
            try {
                if (onlyIfDue && !isDue()) return@launch
                if (refreshReleaseDates() is AppResult.Success) recordRefresh()
            } finally {
                running.unlock()
            }
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
