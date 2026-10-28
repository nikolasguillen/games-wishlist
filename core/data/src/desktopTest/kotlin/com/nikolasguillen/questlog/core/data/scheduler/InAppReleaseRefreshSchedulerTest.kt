package com.nikolasguillen.questlog.core.data.scheduler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * [InAppReleaseRefreshScheduler] has no operating-system job behind it: what is checked is when it asks the
 * [ReleaseRefreshRunner] to run and with which `onlyIfDue`. The refresh rules themselves are the runner's own.
 */
class InAppReleaseRefreshSchedulerTest {

    private val runner = mockk<ReleaseRefreshRunner>()

    init {
        coEvery { runner.run(any()) } returns ReleaseRefreshOutcome.REFRESHED
    }

    @Test
    fun `the launch refresh asks only if due`() = runTest {
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope)

        scheduler.schedulePeriodicRefresh()
        runCurrent()

        coVerify(exactly = 1) { runner.run(true) }
        coVerify(exactly = 0) { runner.run(false) }
    }

    @Test
    fun `the refresh is looked at again every interval while the scope lives`() = runTest {
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope, recheckInterval = 1.hours)

        scheduler.schedulePeriodicRefresh()
        runCurrent()
        advanceTimeBy(59.minutes)
        coVerify(exactly = 1) { runner.run(true) }

        advanceTimeBy(1.minutes + 1.hours)
        runCurrent()

        coVerify(exactly = 3) { runner.run(true) }
    }

    @Test
    fun `scheduling twice does not start a second loop`() = runTest {
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope, recheckInterval = 1.hours)

        scheduler.schedulePeriodicRefresh()
        scheduler.schedulePeriodicRefresh()
        runCurrent()

        coVerify(exactly = 1) { runner.run(true) }
    }

    @Test
    fun `the loop stops when the scope is cancelled`() = runTest {
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope, recheckInterval = 1.hours)
        scheduler.schedulePeriodicRefresh()
        runCurrent()

        backgroundScope.cancel()
        advanceTimeBy(5.hours)
        runCurrent()

        coVerify(exactly = 1) { runner.run(true) }
    }

    @Test
    fun `an immediate refresh ignores the due time`() = runTest {
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope)

        scheduler.scheduleImmediateRefresh()
        runCurrent()

        coVerify(exactly = 1) { runner.run(false) }
        coVerify(exactly = 0) { runner.run(true) }
    }

    @Test
    fun `a failing run does not stop the loop`() = runTest {
        coEvery { runner.run(any()) } throws IllegalStateException("network") andThen ReleaseRefreshOutcome.REFRESHED
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope, recheckInterval = 1.hours)

        scheduler.schedulePeriodicRefresh()
        runCurrent()
        advanceTimeBy(1.hours)
        runCurrent()

        coVerify(exactly = 2) { runner.run(true) }
    }

    @Test
    fun `a refresh cancelled inside the runner does not stop the loop`() = runTest {
        coEvery { runner.run(any()) } throws CancellationException("refresh cancelled") andThen
            ReleaseRefreshOutcome.REFRESHED
        val scheduler = InAppReleaseRefreshScheduler(runner, backgroundScope, recheckInterval = 1.hours)

        scheduler.schedulePeriodicRefresh()
        runCurrent()
        advanceTimeBy(1.hours)
        runCurrent()

        coVerify(exactly = 2) { runner.run(true) }
    }
}
