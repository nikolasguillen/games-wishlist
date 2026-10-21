package com.nikolasguillen.questlog.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val RED = 0xFFC8102E.toInt()
private const val YELLOW = 0xFFFFD500.toInt()
private const val CLEAR = 0x00000000

/** A logo is light or dark by what it draws; the transparent part counts for nothing. */
class LogoToneAnalysisTest {

    @Test
    fun `white artwork on a transparent background is light`() {
        assertEquals(LogoTone.LIGHT, analyseLogoTone(intArrayOf(CLEAR, WHITE, WHITE, CLEAR)))
    }

    @Test
    fun `black artwork on a transparent background is dark`() {
        assertEquals(LogoTone.DARK, analyseLogoTone(intArrayOf(CLEAR, BLACK, BLACK, CLEAR)))
    }

    @Test
    fun `an opaque logo is judged by all of it, background included`() {
        assertEquals(LogoTone.DARK, analyseLogoTone(intArrayOf(BLACK, BLACK, BLACK, WHITE)))
        assertEquals(LogoTone.LIGHT, analyseLogoTone(intArrayOf(WHITE, WHITE, WHITE, BLACK)))
    }

    @Test
    fun `a saturated dark colour is dark and a bright one is light`() {
        assertEquals(LogoTone.DARK, analyseLogoTone(intArrayOf(RED, RED)))
        assertEquals(LogoTone.LIGHT, analyseLogoTone(intArrayOf(YELLOW, YELLOW)))
    }

    @Test
    fun `soft anti-aliasing edges do not count`() {
        // Half-transparent white around black artwork: only the black is part of the logo.
        assertEquals(LogoTone.DARK, analyseLogoTone(intArrayOf(0x80FFFFFF.toInt(), BLACK, BLACK, 0x80FFFFFF.toInt())))
    }

    @Test
    fun `a picture with nothing drawn has no tone`() {
        assertNull(analyseLogoTone(IntArray(16) { CLEAR }))
    }

    @Test
    fun `an empty picture has no tone`() {
        assertNull(analyseLogoTone(IntArray(0)))
    }
}
