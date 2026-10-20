package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SIZE = 10

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val RED = 0xFFC8102E.toInt()
private const val CLEAR = 0x00000000

private val LightTile = Color(0xFFF4F4F4)
private val DarkTile = Color(0xFF282828)

/** A [SIZE] square filled with [background], with a [SIZE] - 4 square of [artwork] in the middle. */
private fun logo(background: Int, artwork: Int): IntArray = IntArray(SIZE * SIZE) { index ->
    val x = index % SIZE
    val y = index / SIZE
    if (x in 2 until SIZE - 2 && y in 2 until SIZE - 2) artwork else background
}

/** What a logo's edge says about its tile: the colour of an opaque edge, the tone of a transparent one. */
class LogoBackdropAnalysisTest {

    @Test
    fun `an opaque edge of one colour gives that colour`() {
        val result = analyseLogoPixels(logo(background = BLACK, artwork = WHITE), SIZE, SIZE)

        assertEquals(LogoBackdrop.Solid(Color(0xFF000000)), result)
    }

    @Test
    fun `a colour that varies slightly across the edge still counts as one colour`() {
        val pixels = logo(background = RED, artwork = WHITE)
        // Compression noise: a few edge pixels one step away in two channels.
        pixels[0] = 0xFFC9112FL.toInt()
        pixels[3] = 0xFFC70F2DL.toInt()

        val result = analyseLogoPixels(pixels, SIZE, SIZE)

        assertTrue(result is LogoBackdrop.Solid)
    }

    @Test
    fun `the edge colour is averaged, not the artwork inside it`() {
        val result = analyseLogoPixels(logo(background = RED, artwork = BLACK), SIZE, SIZE) as LogoBackdrop.Solid

        assertEquals(Color(0xFFC8102E), result.color)
    }

    @Test
    fun `an edge of two colours with no majority is undetermined`() {
        val pixels = logo(background = BLACK, artwork = WHITE)
        // The ring has 36 pixels: 10 turn white and 11 red, so the 15 black ones (42%) are not a majority.
        for (x in 0 until SIZE) pixels[x] = WHITE
        for (y in 1 until SIZE - 1) pixels[y * SIZE] = RED
        for (y in 1..3) pixels[y * SIZE + SIZE - 1] = RED

        assertEquals(LogoBackdrop.Undetermined, analyseLogoPixels(pixels, SIZE, SIZE))
    }

    @Test
    fun `a transparent edge gives the luminance of the artwork`() {
        val result = analyseLogoPixels(logo(background = CLEAR, artwork = WHITE), SIZE, SIZE)

        assertEquals(LogoBackdrop.Artwork(luminance = 1f), result)
    }

    @Test
    fun `dark artwork on a transparent edge has a low luminance`() {
        val result = analyseLogoPixels(logo(background = CLEAR, artwork = BLACK), SIZE, SIZE) as LogoBackdrop.Artwork

        assertEquals(0f, result.luminance, 0.001f)
    }

    @Test
    fun `an edge that is partly transparent is undetermined`() {
        val pixels = logo(background = BLACK, artwork = WHITE)
        // 5 of the ring's 36 pixels (14%) are clear: some transparency, but not a transparent logo.
        for (x in 0 until 5) pixels[x] = CLEAR

        assertEquals(LogoBackdrop.Undetermined, analyseLogoPixels(pixels, SIZE, SIZE))
    }

    @Test
    fun `a picture with nothing drawn is undetermined`() {
        assertEquals(LogoBackdrop.Undetermined, analyseLogoPixels(IntArray(SIZE * SIZE) { CLEAR }, SIZE, SIZE))
    }

    @Test
    fun `a one pixel picture is read without failing`() {
        assertEquals(LogoBackdrop.Solid(Color(0xFF000000)), analyseLogoPixels(intArrayOf(BLACK), 1, 1))
    }

    @Test
    fun `a buffer smaller than its size is undetermined`() {
        assertEquals(LogoBackdrop.Undetermined, analyseLogoPixels(IntArray(3), SIZE, SIZE))
    }

    @Test
    fun `a solid backdrop is the tile whatever the theme`() {
        val backdrop = LogoBackdrop.Solid(Color.Black)

        assertEquals(Color.Black, backdrop.resolveLogoTileColor(LightTile, DarkTile))
        assertEquals(Color.Black, backdrop.resolveLogoTileColor(DarkTile, LightTile))
    }

    @Test
    fun `an undetermined backdrop keeps the theme tile`() {
        assertEquals(LightTile, LogoBackdrop.Undetermined.resolveLogoTileColor(LightTile, DarkTile))
        assertEquals(DarkTile, LogoBackdrop.Undetermined.resolveLogoTileColor(DarkTile, LightTile))
    }

    @Test
    fun `white artwork moves off the light theme tile`() {
        val white = LogoBackdrop.Artwork(luminance = 1f)

        assertEquals(DarkTile, white.resolveLogoTileColor(themeTile = LightTile, contrastTile = DarkTile))
    }

    @Test
    fun `black artwork moves off the dark theme tile`() {
        val black = LogoBackdrop.Artwork(luminance = 0f)

        assertEquals(LightTile, black.resolveLogoTileColor(themeTile = DarkTile, contrastTile = LightTile))
    }

    @Test
    fun `artwork that reads on the theme tile keeps it`() {
        val black = LogoBackdrop.Artwork(luminance = 0f)
        val white = LogoBackdrop.Artwork(luminance = 1f)

        assertEquals(LightTile, black.resolveLogoTileColor(themeTile = LightTile, contrastTile = DarkTile))
        assertEquals(DarkTile, white.resolveLogoTileColor(themeTile = DarkTile, contrastTile = LightTile))
    }
}
