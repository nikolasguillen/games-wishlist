package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ReleaseReminderCoordinator] is what makes the pending reminders follow the app's state on iOS, where the system
 * fires them without running app code: a sync at launch, on every trigger event (foreground, permission granted),
 * and for a game that leaves the saved set.
 */
class ReleaseReminderCoordinatorTest {

    private val sync = mockk<SyncReleaseNotificationsUseCase>(relaxed = true)
    private val triggerEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    private val savedIds = MutableStateFlow(setOf(1, 2))

    private val triggers = object : ReminderSyncTriggers {
        override val events: Flow<Unit> = triggerEvents
    }

    private fun TestScope.coordinator() = ReleaseReminderCoordinator(
        syncReleaseNotifications = sync,
        getSavedGameIds = { savedIds },
        triggers = triggers,
        scope = backgroundScope
    )

    @Test
    fun `start runs one full sync at once`() = runTest {
        coordinator().start()
        runCurrent()

        coVerify(exactly = 1) { sync(null) }
    }

    @Test
    fun `each trigger event runs another full sync`() = runTest {
        coordinator().start()
        runCurrent()

        triggerEvents.tryEmit(Unit)
        runCurrent()

        coVerify(exactly = 2) { sync(null) }
    }

    @Test
    fun `a permission granted event arms a reminder that was refused while permission was undecided`() = runTest {
        // The first sync, at launch, finds permission undecided and arms nothing; the granted event syncs again.
        var armed = false
        var permissionGranted = false
        coEvery { sync(null) } coAnswers { armed = permissionGranted }
        coordinator().start()
        runCurrent()

        permissionGranted = true
        triggerEvents.tryEmit(Unit)
        runCurrent()

        assertTrue(armed)
    }

    @Test
    fun `the first emission of the saved ids is a baseline and syncs nothing`() = runTest {
        coordinator().start()
        runCurrent()

        coVerify(exactly = 0) { sync(1) }
        coVerify(exactly = 0) { sync(2) }
    }

    @Test
    fun `an id that leaves the saved set syncs that game once`() = runTest {
        coordinator().start()
        runCurrent()

        savedIds.value = setOf(2)
        runCurrent()

        coVerify(exactly = 1) { sync(1) }
        coVerify(exactly = 0) { sync(2) }
    }

    @Test
    fun `an id that comes back to the saved set syncs nothing`() = runTest {
        coordinator().start()
        runCurrent()
        savedIds.value = setOf(2)
        runCurrent()

        savedIds.value = setOf(1, 2)
        runCurrent()

        coVerify(exactly = 1) { sync(1) }
    }

    @Test
    fun `trigger events during a running sync produce exactly one follow-up full sync`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { sync(null) } coAnswers { if (calls++ == 0) gate.await() }
        coordinator().start()
        runCurrent()

        repeat(5) { triggerEvents.tryEmit(Unit) }
        runCurrent()
        coVerify(exactly = 1) { sync(null) }

        gate.complete(Unit)
        runCurrent()

        coVerify(exactly = 2) { sync(null) }
    }

    @Test
    fun `calling start twice does not double the work`() = runTest {
        val coordinator = coordinator()
        coordinator.start()
        coordinator.start()
        runCurrent()
        coVerify(exactly = 1) { sync(null) }

        triggerEvents.tryEmit(Unit)
        runCurrent()

        coVerify(exactly = 2) { sync(null) }
    }

    @Test
    fun `a failing sync does not stop later syncs`() = runTest {
        var calls = 0
        coEvery { sync(null) } coAnswers { if (calls++ == 0) error("boom") }
        coordinator().start()
        runCurrent()

        triggerEvents.tryEmit(Unit)
        runCurrent()

        coVerify(exactly = 2) { sync(null) }
    }

    @Test
    fun `a cancellation from a sync is not swallowed`() = runTest {
        coEvery { sync(null) } throws CancellationException("cancelled")
        coordinator().start()
        runCurrent()

        triggerEvents.tryEmit(Unit)
        runCurrent()

        coVerify(exactly = 1) { sync(null) }
    }
}
