package com.nikolasguillen.questlog.core.common.desktop.windows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class ToastXmlTest {

    @Test
    fun `title and body are escaped`() {
        val xml = ToastXml.build(1, title = "A & B <C>", body = "\"quoted\" and 'single'")

        assertTrue(xml, "<text>A &amp; B &lt;C&gt;</text>" in xml)
        assertTrue(xml, "<text>&quot;quoted&quot; and &apos;single&apos;</text>" in xml)
    }

    @Test
    fun `the toast carries the game id as its launch argument`() {
        val xml = ToastXml.build(123, "t", "b")

        assertTrue(xml, xml.startsWith("""<toast launch="gameId=123" activationType="foreground">"""))
        assertTrue(xml, """<binding template="ToastGeneric">""" in xml)
    }

    @Test
    fun `the toast id is g plus the game id and stays within 16 characters`() {
        assertEquals("g42", ToastXml.toastId(42))
        assertTrue(ToastXml.toastId(Int.MAX_VALUE).length <= 16)
    }

    @Test
    fun `only an id built by toastId maps back to a game`() {
        assertEquals(42, ToastXml.gameIdOf("g42"))
        assertNull(ToastXml.gameIdOf("x42"))
        assertNull(ToastXml.gameIdOf("g"))
        assertNull(ToastXml.gameIdOf("gabc"))
        assertNull(ToastXml.gameIdOf("42"))
    }

    @Test
    fun `the unix epoch is 116444736000000000 ticks since 1601`() {
        assertEquals(116_444_736_000_000_000L, ToastXml.toTicks(Instant.fromEpochSeconds(0)))
    }

    @Test
    fun `a known instant converts to ticks and back`() {
        // 2027-03-04T09:00:00Z is 1804064400 seconds after the unix epoch.
        val instant = Instant.fromEpochSeconds(1_804_064_400, nanosecondAdjustment = 500_000_000)

        val ticks = ToastXml.toTicks(instant)

        assertEquals((1_804_064_400L + 11_644_473_600L) * 10_000_000L + 5_000_000L, ticks)
        assertEquals(instant, ToastXml.fromTicks(ticks))
    }
}
