package com.nikolasguillen.questlog.core.common.desktop.windows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivationArgumentsTest {

    @Test
    fun `a game id is parsed`() {
        assertEquals(123, parseGameId("gameId=123"))
    }

    @Test
    fun `anything else is rejected`() {
        listOf("gameId=", "gameId=abc", "gameId=-1", "gameId=0", "gameId=1.5", "gameId=99999999999", "", "other=1", "gameId=1&x=2")
            .forEach { assertNull(it, parseGameId(it)) }
    }
}
