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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 * [ReleaseRefreshRunner] holds the rules every release-date refresh on iOS obeys, whether the app or the system
 * started it: when a refresh is due, that the time is recorded only after a success, that one refresh runs at a
 * time, and when the next background request may start.
 */
class ReleaseRefreshRunnerTest {

    private val lastRefreshKey = longPreferencesKey("release_dates_last_refresh_epoch_ms")
    private val start = Instant.fromEpochSeconds(1_700_000_000)

    private val refreshReleaseDates = mockk<RefreshReleaseDatesUseCase>()
    private val clock = FakeClock(start)

    private fun dataStore(): DataStore<Preferences> = InMemoryPreferencesDataStore()

    private fun TestScope.runner(dataStore: DataStore<Preferences>) =
        ReleaseRefreshRunner(refreshReleaseDates, dataStore, scope = this, clock = clock)

    private suspend fun DataStore<Preferences>.lastRefreshMillis(): Long? = data.first()[lastRefreshKey]

    private suspend fun DataStore<Preferences>.setLastRefresh(at: Instant) {
        edit { it[lastRefreshKey] = at.toEpochMilliseconds() }
    }

    @Test
    fun `the first launch refreshes, because nothing was ever refreshed`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)

        val outcome = runner(dataStore()).run(onlyIfDue = true)

        assertEquals(ReleaseRefreshOutcome.REFRESHED, outcome)
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a refresh exactly 24 hours old is due`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - 24.hours) }

        val outcome = runner(dataStore).run(onlyIfDue = true)

        assertEquals(ReleaseRefreshOutcome.REFRESHED, outcome)
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a refresh from 23 hours 59 minutes ago is not due and makes no request`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - (23.hours + 59.minutes)) }

        val outcome = runner(dataStore).run(onlyIfDue = true)

        assertEquals(ReleaseRefreshOutcome.NOT_DUE, outcome)
        coVerify(exactly = 0) { refreshReleaseDates() }
    }

    @Test
    fun `a successful refresh records the time it finished`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore()

        runner(dataStore).run(onlyIfDue = true)

        assertEquals(start.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }

    @Test
    fun `an immediate refresh ignores how recent the last one was`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val dataStore = dataStore().also { it.setLastRefresh(start - 1.minutes) }

        val outcome = runner(dataStore).run(onlyIfDue = false)

        assertEquals(ReleaseRefreshOutcome.REFRESHED, outcome)
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `the next request may start now when nothing was ever refreshed`() = runTest {
        assertEquals(start, runner(dataStore()).nextEarliestBegin())
    }

    @Test
    fun `the next request may start now when the last refresh is overdue`() = runTest {
        val dataStore = dataStore().also { it.setLastRefresh(start - 30.hours) }

        assertEquals(start, runner(dataStore).nextEarliestBegin())
    }

    @Test
    fun `the next request waits until 24 hours after the last refresh`() = runTest {
        val lastRefresh = start - 5.hours
        val dataStore = dataStore().also { it.setLastRefresh(lastRefresh) }

        assertEquals(lastRefresh + 24.hours, runner(dataStore).nextEarliestBegin())
    }

    @Test
    fun `a failed refresh does not advance the timestamp, so the next launch tries again`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.NoNetwork)
        val dataStore = dataStore()

        val outcome = runner(dataStore).run(onlyIfDue = true)

        assertEquals(ReleaseRefreshOutcome.FAILED, outcome)
        assertNull(dataStore.lastRefreshMillis())
    }

    @Test
    fun `a failed refresh leaves an earlier timestamp as it was`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.failure(RepositoryError.RequestTimeout)
        val earlier = start - 30.hours
        val dataStore = dataStore().also { it.setLastRefresh(earlier) }

        runner(dataStore).run(onlyIfDue = true)

        assertEquals(earlier.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }

    @Test
    fun `a cancelled refresh records nothing and does not block the next one`() = runTest {
        var calls = 0
        coEvery { refreshReleaseDates() } coAnswers {
            if (++calls == 1) throw CancellationException("scope closed")
            AppResult.success(Unit)
        }
        val dataStore = dataStore()
        val runner = runner(dataStore)

        val cancelled = runCatching { runner.run(onlyIfDue = false) }.exceptionOrNull()
        assertTrue(cancelled is CancellationException)
        assertNull(dataStore.lastRefreshMillis())

        assertEquals(ReleaseRefreshOutcome.REFRESHED, runner.run(onlyIfDue = false))
        assertEquals(start.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }

    @Test
    fun `cancelling the running refresh stops the use case and records nothing`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val dataStore = dataStore()
        val runner = runner(dataStore)

        val caller = async { runCatching { runner.run(onlyIfDue = false) } }
        advanceUntilIdle()
        runner.cancelRunning()
        advanceUntilIdle()

        assertTrue(caller.await().exceptionOrNull() is CancellationException)
        assertNull(dataStore.lastRefreshMillis())
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `cancelling when nothing is running does nothing`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val runner = runner(dataStore())

        runner.cancelRunning()

        assertEquals(ReleaseRefreshOutcome.REFRESHED, runner.run(onlyIfDue = true))
    }

    @Test
    fun `a request while a refresh is running joins it and gets the same outcome`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val runner = runner(dataStore())

        val first = async { runner.run(onlyIfDue = false) }
        advanceUntilIdle()
        val second = async { runner.run(onlyIfDue = false) }
        val third = async { runner.run(onlyIfDue = true) }
        advanceUntilIdle()
        gate.complete(Unit)

        assertEquals(
            listOf(ReleaseRefreshOutcome.REFRESHED, ReleaseRefreshOutcome.REFRESHED, ReleaseRefreshOutcome.REFRESHED),
            listOf(first.await(), second.await(), third.await())
        )
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `once a refresh has finished the next request runs`() = runTest {
        coEvery { refreshReleaseDates() } returns AppResult.success(Unit)
        val runner = runner(dataStore())

        runner.run(onlyIfDue = false)
        runner.run(onlyIfDue = false)

        coVerify(exactly = 2) { refreshReleaseDates() }
    }

    @Test
    fun `a caller that is not due still joins a refresh in flight instead of returning not due`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val dataStore = dataStore().also { it.setLastRefresh(start - 1.minutes) }
        val runner = runner(dataStore)

        val inApp = async { runner.run(onlyIfDue = false) }
        advanceUntilIdle()
        val background = async { runner.run(onlyIfDue = true) }
        advanceUntilIdle()
        gate.complete(Unit)

        assertEquals(ReleaseRefreshOutcome.REFRESHED, background.await())
        assertEquals(ReleaseRefreshOutcome.REFRESHED, inApp.await())
        coVerify(exactly = 1) { refreshReleaseDates() }
    }

    @Test
    fun `a caller that stops waiting does not cancel the refresh`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refreshReleaseDates() } coAnswers {
            gate.await()
            AppResult.success(Unit)
        }
        val dataStore = dataStore()
        val runner = runner(dataStore)

        val impatient = launch { runner.run(onlyIfDue = false) }
        advanceUntilIdle()
        impatient.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(start.toEpochMilliseconds(), dataStore.lastRefreshMillis())
    }
}
