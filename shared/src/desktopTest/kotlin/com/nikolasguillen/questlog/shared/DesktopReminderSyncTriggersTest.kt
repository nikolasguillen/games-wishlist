package com.nikolasguillen.questlog.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class DesktopReminderSyncTriggersTest {

    @Test
    fun `the window gaining focus is a sync trigger`() = runBlocking {
        val events = Channel<Unit>(Channel.UNLIMITED)
        // The permission is read off the main thread before each event, so the test waits in real time.
        val job = launch(Dispatchers.Default) { DesktopReminderSyncTriggers().events.collect { events.send(it) } }
        // Focus is a hot signal that is only seen once the collector has subscribed, so it is repeated until one lands.
        repeat(2) {
            withTimeout(5_000) {
                while (events.tryReceive().isFailure) {
                    DesktopWindowFocus.onFocusGained()
                    delay(20)
                }
            }
        }
        job.cancel()
    }
}
