package com.nikolasguillen.questlog.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SingleInstanceMessageTest {

    @Test
    fun `focus parses`() {
        assertEquals(SingleInstanceMessage.Focus, SingleInstanceMessage.parse("FOCUS"))
    }

    @Test
    fun `open game parses to its id`() {
        assertEquals(SingleInstanceMessage.OpenGame(123), SingleInstanceMessage.parse("OPEN_GAME 123"))
    }

    @Test
    fun `an id that is not a positive integer is ignored`() {
        listOf("OPEN_GAME 0", "OPEN_GAME -5", "OPEN_GAME +5", "OPEN_GAME abc", "OPEN_GAME", "OPEN_GAME ", "OPEN_GAME 1 2")
            .forEach { assertNull(it, SingleInstanceMessage.parse(it)) }
    }

    @Test
    fun `an id that does not fit an int is ignored`() {
        assertNull(SingleInstanceMessage.parse("OPEN_GAME 99999999999"))
    }

    @Test
    fun `empty and unknown lines are ignored`() {
        listOf("", " ", "focus", "FOCUS ", "QUIT", "OPEN_GAME123").forEach {
            assertNull(it, SingleInstanceMessage.parse(it))
        }
    }

    @Test
    fun `a message parses back from its own line`() {
        listOf(SingleInstanceMessage.Focus, SingleInstanceMessage.OpenGame(42)).forEach {
            assertEquals(it, SingleInstanceMessage.parse(it.line))
        }
    }
}
