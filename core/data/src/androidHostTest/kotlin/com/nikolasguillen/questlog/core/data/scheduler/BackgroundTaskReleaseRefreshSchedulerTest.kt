package com.nikolasguillen.questlog.core.data.scheduler

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * [BackgroundTaskReleaseRefreshScheduler] is iOS's stand-in for WorkManager: the in-app launch and wishlist
 * refreshes, and the body of the background task the system launches. The refresh rules themselves are
 * [ReleaseRefreshRunner]'s; what is checked here is what the scheduler adds on top - the next background request
 * is submitted before the work starts and again after it, and the system is told the outcome exactly once.
 *
 * An empty wishlist reaches the scheduler as a success from the use case. That it makes no network request is
 * owned by `GameRepositoryImpl.refreshSavedGameReleaseDates` and its own test, not checked here.
 */
class BackgroundTaskReleaseRefreshSchedulerTest {

    private val lastRefreshKey = longPreferencesKey("release_dates_last_refresh_epoch_ms")
    private val start = Instant.fromEpochSeconds(1_700_000_000)

    private val refreshReleaseDates = mockk<RefreshReleaseDatesUseCase>()
    private val clock = FakeClock(start)
    private val bridge = FakeAppRefreshTaskBridge()
    private val dataStore: DataStore<Preferences> = InMemoryPreferencesDataStore()

    private fun TestScope.scheduler() = BackgroundTaskReleaseRefreshScheduler(
        runner = ReleaseRefreshRunner(refreshReleaseDates, dataStore, scope = this, clock = clock),
        bridge = bridge,
        scope = this
    )

    private suspend fun lastRefreshMillis(): Long? = dataStore.data.first()[lastRefreshKey]

    private suspend fun setLastRefresh(at: Instant) {
        dataStore.edit { it[lastRefreshKey] = at.toEpochMilliseconds() }
    }

    @Test
    fun `a due launch submits, refreshes, then submits at 24 hours after the new success`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)

        scheduler().schedulePeriodicRefresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
        assertEquals(listOf(start, start + 24.hours), bridge.submitted)
    }

    @Test
    fun `a launch that is not due makes no request and still submits the next one`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        setLastRefresh(start - 2.hours)

        scheduler().schedulePeriodicRefresh()
        advanceUntilIdle()

        coVerify(exactly = 0) { refreshReleaseDates() }
        val expected = start + 22.hours
        assertEquals(listOf(expected, expected), bridge.submitted)
    }

    @Test
    fun `an immediate refresh runs whatever the last one was and submits`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        setLastRefresh(start - 1.minutes)

        scheduler().scheduleImmediateRefresh()
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
        assertEquals(start + 24.hours, bridge.submitted.last())
    }

    @Test
    fun `the next request is already pending while the refresh runs, and the new one replaces it`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }

        scheduler().schedulePeriodicRefresh()
        advanceUntilIdle()
        assertEquals(listOf(start), bridge.submitted)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(start, start + 24.hours), bridge.submitted)
    }

    @Test
    fun `a background run refreshes once and reports success after the final submit`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val reports = mutableListOf<Pair<Boolean, Int>>()

        scheduler().run { ok -> reports += ok to bridge.submitted.size }
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
        assertEquals(start.toEpochMilliseconds(), lastRefreshMillis())
        assertEquals(listOf(true to 2), reports)
    }

    @Test
    fun `a background run that is not due makes no request and reports success`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        setLastRefresh(start - 2.hours)
        val reports = mutableListOf<Boolean>()

        scheduler().run { ok -> reports += ok }
        advanceUntilIdle()

        coVerify(exactly = 0) { refreshReleaseDates() }
        assertEquals(listOf(true), reports)
        assertTrue(bridge.submitted.isNotEmpty())
    }

    @Test
    fun `a failed background run reports failure once and still submits after the run`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.NoNetwork)
        val reports = mutableListOf<Pair<Boolean, Int>>()

        scheduler().run { ok -> reports += ok to bridge.submitted.size }
        advanceUntilIdle()

        assertEquals(listOf(false to 2), reports)
        assertNull(lastRefreshMillis())
    }

    @Test
    fun `cancelling a background run ends it, submits, reports failure once and records nothing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val reports = mutableListOf<Pair<Boolean, Int>>()
        val run = scheduler().run { ok -> reports += ok to bridge.submitted.size }
        advanceUntilIdle()

        run.cancel()
        advanceUntilIdle()

        assertEquals(listOf(false to 2), reports)
        assertNull(lastRefreshMillis())
    }

    @Test
    fun `a run cancelled before it first executes still reports failure`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val reports = mutableListOf<Boolean>()

        scheduler().run { ok -> reports += ok }.cancel()
        advanceUntilIdle()

        assertEquals(listOf(false), reports)
        assertNull(lastRefreshMillis())
    }

    @Test
    fun `cancelling twice, or after the run finished, does nothing more`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val reports = mutableListOf<Boolean>()
        val run = scheduler().run { ok -> reports += ok }
        advanceUntilIdle()

        run.cancel()
        run.cancel()
        advanceUntilIdle()
        run.cancel()
        advanceUntilIdle()

        assertEquals(listOf(false), reports)
    }

    @Test
    fun `cancelling a finished run does not cancel a later refresh`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } returnsMany listOf(AppResult.success(Unit)) coAndThen {
            gate.await()
            AppResult.success(Unit)
        }
        val scheduler = scheduler()
        val finished = scheduler.run { }
        advanceUntilIdle()
        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()

        finished.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(start.toEpochMilliseconds(), lastRefreshMillis())
        coVerify(exactly = 2) { refreshReleaseDates() }
    }

    @Test
    fun `a failed in-app refresh still submits the next request`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.RequestTimeout)

        scheduler().scheduleImmediateRefresh()
        advanceUntilIdle()

        assertEquals(listOf(start, start), bridge.submitted)
        assertNull(lastRefreshMillis())
    }

    @Test
    fun `a background run that starts during an in-app refresh waits for it instead of reporting early`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val reports = mutableListOf<Boolean>()
        val scheduler = scheduler()

        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        scheduler.run { ok -> reports += ok }
        advanceUntilIdle()
        assertTrue(reports.isEmpty())

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf(true), reports)
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `in-app requests during a background refresh join it instead of refreshing again`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val scheduler = scheduler()

        scheduler.run { }
        advanceUntilIdle()
        scheduler.scheduleImmediateRefresh()
        scheduler.schedulePeriodicRefresh()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `cancelling a background run that joined an in-app refresh cancels that refresh`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val reports = mutableListOf<Boolean>()
        val scheduler = scheduler()
        scheduler.scheduleImmediateRefresh()
        advanceUntilIdle()
        val run = scheduler.run { ok -> reports += ok }
        advanceUntilIdle()

        run.cancel()
        advanceUntilIdle()

        assertEquals(listOf(false), reports)
        assertNull(lastRefreshMillis())
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    // What an empty wishlist yields is a success from the use case; that it makes no network request is owned by
    // GameRepositoryImpl.refreshSavedGameReleaseDates and its own test.
    @Test
    fun `an empty wishlist, which the use case answers with success, counts as a refresh`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val reports = mutableListOf<Pair<Boolean, Int>>()

        scheduler().run { ok -> reports += ok to bridge.submitted.size }
        advanceUntilIdle()

        assertEquals(listOf(true to 2), reports)
        assertEquals(start.toEpochMilliseconds(), lastRefreshMillis())
        assertEquals(start + 24.hours, bridge.submitted.last())
    }
}
