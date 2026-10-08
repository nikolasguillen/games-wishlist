package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [parseHtml], which turns a formatted string resource into styled text. Its one real caller is the
 * "remove from recent searches" message, which bolds the search term with `<b>`.
 */
class HtmlUtilsTest {

    private data class Span(val start: Int, val end: Int, val style: SpanStyle)

    private fun String.spans() = parseHtml().spanStyles.map { Span(it.start, it.end, it.item) }.sortedBy { it.start }

    private val bold = SpanStyle(fontWeight = FontWeight.Bold)
    private val italic = SpanStyle(fontStyle = FontStyle.Italic)
    private val underline = SpanStyle(textDecoration = TextDecoration.Underline)

    @Test
    fun `bold text loses its tags and keeps a bold span over the word`() {
        val parsed = "Remove <b>Zelda</b> now".parseHtml()

        assertEquals("Remove Zelda now", parsed.text)
        assertEquals(listOf(Span(7, 12, bold)), "Remove <b>Zelda</b> now".spans())
    }

    @Test
    fun `italic and underline are supported`() {
        assertEquals(listOf(Span(0, 2, italic)), "<i>hi</i>".spans())
        assertEquals(listOf(Span(2, 4, underline)), "a <u>bc</u>".spans())
    }

    @Test
    fun `tags nest`() {
        val parsed = "<b>very <i>bold</i></b>".parseHtml()

        assertEquals("very bold", parsed.text)
        assertEquals(listOf(Span(0, 9, bold), Span(5, 9, italic)), "<b>very <i>bold</i></b>".spans())
    }

    @Test
    fun `tag names are case-insensitive`() {
        assertEquals("x", "<B>x</B>".parseHtml().text)
        assertEquals(listOf(Span(0, 1, bold)), "<B>x</B>".spans())
    }

    @Test
    fun `text without tags comes back unchanged with no spans`() {
        val parsed = "Nothing to see here".parseHtml()

        assertEquals("Nothing to see here", parsed.text)
        assertTrue(parsed.spanStyles.isEmpty())
    }

    @Test
    fun `a stray angle bracket stays literal`() {
        val parsed = "3 < 4 and 5 > 2".parseHtml()

        assertEquals("3 < 4 and 5 > 2", parsed.text)
        assertTrue(parsed.spanStyles.isEmpty())
    }

    @Test
    fun `unknown, unclosed and unopened tags stay literal`() {
        assertEquals("<span>x</span>", "<span>x</span>".parseHtml().text)
        assertEquals("<b>never closed", "<b>never closed".parseHtml().text)
        assertEquals("never opened</b>", "never opened</b>".parseHtml().text)
        assertTrue("<b>never closed".parseHtml().spanStyles.isEmpty())
    }
}
