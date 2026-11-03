package com.nikolasguillen.questlog.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopReminderSyncTriggersTest {

    @Test
    fun `the window gaining focus is a sync trigger`() = runTest {
        val events = mutableListOf<Unit>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            DesktopReminderSyncTriggers().events.collect { events += it }
        }

        DesktopWindowFocus.onFocusGained()
        DesktopWindowFocus.onFocusGained()

        assertEquals(2, events.size)
        job.cancel()
    }
}
