package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure rules of the wide Discover layout: how many cards a shelf row holds at a width, when a shelf shows
 * its "See all" link, and how tall the hero banner is for a window and a banner width.
 */
class WideDiscoverLayoutTest {

    private val spacing = 16.dp
    private val edge = 16.dp

    private fun columns(width: Int) = wideShelfColumnCount(width.dp, spacing, edge)

    private fun heroHeight(windowHeight: Int, width: Int) = wideHeroHeight(windowHeight.dp, width.dp).value

    @Test
    fun `six cards fit from 1072dp`() {
        // 6 * 160 card + 5 * 16 gaps + 2 * 16 edges = 1072.
        assertEquals(6, columns(1072))
        assertEquals(6, columns(1080))
    }

    @Test
    fun `a sixth card needs the full 1072dp`() {
        assertEquals(5, columns(1071))
    }

    @Test
    fun `narrower rows hold fewer cards`() {
        assertEquals(4, columns(760))
    }

    @Test
    fun `a row holds at least one card`() {
        assertEquals(1, columns(100))
        assertEquals(1, columns(0))
    }

    @Test
    fun `see all shows when the shelf has more games than it carries`() {
        assertTrue(wideShelfShowsSeeAll(hasMore = true, gameCount = 3, columns = 6))
    }

    @Test
    fun `see all shows when the width hides some of the games`() {
        assertTrue(wideShelfShowsSeeAll(hasMore = false, gameCount = 6, columns = 4))
    }

    @Test
    fun `see all stays hidden when every game is on screen and the shelf has no more`() {
        assertFalse(wideShelfShowsSeeAll(hasMore = false, gameCount = 6, columns = 6))
        assertFalse(wideShelfShowsSeeAll(hasMore = false, gameCount = 3, columns = 6))
    }

    @Test
    fun `hero is 360dp at an 800dp tall wide window`() {
        assertEquals(360f, heroHeight(windowHeight = 800, width = 1150), 0.01f)
    }

    @Test
    fun `hero is clearly taller than the phone hero from 800dp up`() {
        listOf(800, 900, 1080, 1440).forEach { windowHeight ->
            assertTrue(
                "window $windowHeight",
                heroHeight(windowHeight, width = 2440) >= 1.3f * 240f
            )
        }
    }

    @Test
    fun `hero stays within the floor and the ceiling when the width cap does not bite`() {
        assertEquals(280f, heroHeight(windowHeight = 600, width = 2440), 0.01f)
        assertEquals(560f, heroHeight(windowHeight = 1440, width = 2440), 0.01f)
    }

    @Test
    fun `hero is capped by the banner width`() {
        assertEquals(376f, heroHeight(windowHeight = 900, width = 470), 0.01f)
    }

    @Test
    fun `the width cap wins over the floor`() {
        assertEquals(240f, heroHeight(windowHeight = 900, width = 300), 0.01f)
    }
}
