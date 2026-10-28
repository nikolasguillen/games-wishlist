package com.nikolasguillen.questlog.desktop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * A second `acquireOrHandOver` in the same JVM meets the first one's lock exactly as a second process would, so the
 * hand-over is checked here without starting one.
 */
class SingleInstanceTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `the first caller becomes the instance and a later one hands over`() = runBlocking {
        val directory = folder.root.toPath()
        val first = SingleInstance.acquireOrHandOver(SingleInstanceMessage.Focus, directory)
        assertNotNull(first)

        first!!.use {
            val second = SingleInstance.acquireOrHandOver(SingleInstanceMessage.OpenGame(7), directory)
            assertNull(second)

            assertEquals(SingleInstanceMessage.OpenGame(7), withTimeout(5_000) { first.messages.first() })
        }
    }

    @Test
    fun `the instance can be taken again once it is closed`() {
        val directory = folder.root.toPath()
        SingleInstance.acquireOrHandOver(SingleInstanceMessage.Focus, directory)!!.close()

        val again = SingleInstance.acquireOrHandOver(SingleInstanceMessage.Focus, directory)

        assertNotNull(again)
        again!!.close()
    }

    @Test
    fun `a stale socket file does not stop the first caller`() {
        val directory = folder.root.toPath()
        folder.newFile("instance.sock")

        val instance = SingleInstance.acquireOrHandOver(SingleInstanceMessage.Focus, directory)

        assertNotNull(instance)
        instance!!.close()
    }
}
