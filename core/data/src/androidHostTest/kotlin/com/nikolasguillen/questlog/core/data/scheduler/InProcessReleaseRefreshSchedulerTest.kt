package com.nikolasguillen.questlog.core.data.scheduler

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * [InProcessReleaseRefreshScheduler] is iOS's stand-in for WorkManager, which has no counterpart there: it runs
 * the refresh inside the app, at launch and when the saved set changes. What it has to get right is the same
 * as the Android job - the 24h cadence, one run at a time - plus one thing WorkManager does for free: remember
 * when the last refresh *succeeded*, so a failed or interrupted run is retried at the next launch instead of
 * being counted.
 */
class InProcessReleaseRefreshSchedulerTest {

    private val lastRefreshKey = longPreferencesKey("release_dates_last_refresh_epoch_ms")
    private val start = Instant.fromEpochSeconds(1_700_000_000)

    private val refreshReleaseDates = mockk<RefreshReleaseDatesUseCase>()

    private class FakeClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    private val clock = FakeClock(start)

    // In memory rather than file-backed: a real DataStore does its I/O on real threads, which the test scheduler
    // cannot wait for, so the assertions would race the write.
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    private fun dataStore(): DataStore<Preferences> = InMemoryPreferencesDataStore()

    private fun TestScope.scheduler(dataStore: DataStore<Preferences>) =
        InProcessReleaseRefreshScheduler(refreshReleaseDates, dataStore, scope = this, clock = clock)

    private suspend fun DataStore<Preferences>.lastRefreshMillis(): Long? = data.first()[lastRefreshKey]

    private suspend fun DataStore<Preferences>.setLastRefresh(at: Instant) {
        edit { it[lastRefreshKey] = at.toEpochMilliseconds() }
    }

    @Test
    fun `the first launch refreshes, because nothing was ever refreshed`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore()

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a refresh older than 24 hours is due`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - 24.hours) }

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a refresh from 23 hours 59 minutes ago is not due`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - (23.hours + 59.minutes)) }

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        coVerify(exactly = 0) { refreshReleaseDates() }
    }

    @Test
    fun `a successful refresh records the time it finished`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore()

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        assertEquals(start.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }

    @Test
    fun `an immediate refresh ignores how recent the last one was`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - 1.minutes) }

        scheduler(dataStore).scheduleImmediateRefresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a failed refresh does not advance the timestamp, so the next launch tries again`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.NoNetwork)
        val dataStore = dataStore()

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        assertNull(dataStore.lastRefreshMillis())
    }

    @Test
    fun `a failed refresh leaves an earlier timestamp as it was`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.RequestTimeout)
        val earlier = start - 30.hours
        val dataStore = dataStore().also { it.setLastRefresh(earlier) }

        scheduler(dataStore).schedulePeriodicRefresh()
        advanceUntilIdle()

        assertEquals(earlier.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }

    @Test
    fun `a request while a refresh is running does not start a second one`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val scheduler = scheduler(dataStore())

        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        scheduler.scheduleImmediateRefresh()
        scheduler.schedulePeriodicRefresh()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `once a refresh has finished the next request runs`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val scheduler = scheduler(dataStore())

        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { refreshReleaseDates() }
    }

    @Test
    fun `a cancelled refresh records nothing and does not block the next one`() = runTest {
        var calls = 0
        coEvery { refreshReleaseDates() } coAnswers {
            if (++calls == 1) throw CancellationException("scope closed")
            AppResult.success(Unit)
        }
        val dataStore = dataStore()
        val scheduler = scheduler(dataStore)

        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        assertNull(dataStore.lastRefreshMillis())

        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        assertEquals(start.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }
}
