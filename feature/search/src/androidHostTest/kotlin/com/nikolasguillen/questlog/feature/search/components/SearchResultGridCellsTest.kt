package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the results grid's geometry: how many columns a width gets (always two in portrait, at least three in
 * landscape) and how wide each column's cell is, so that every card ends up the same visible width at any column
 * count. Everything runs at a density of 1, where a dp is a pixel.
 */
class SearchResultGridCellsTest {

    private val density = Density(1f)
    private val spacing = 16.dp
    private val edge = 16.dp

    private fun columnCount(width: Int, isLandscape: Boolean) =
        resultGridColumnCount(width.dp, spacing, edge, isLandscape)

    private fun cellSizes(columnCount: Int, width: Int): List<Int> =
        with(SearchResultGridCells(columnCount, edge)) {
            with(density) { calculateCrossAxisCellSizes(width, spacing.roundToPx()) }
        }

    @Test
    fun `portrait is always two columns`() {
        listOf(320, 360, 412, 800, 1280).forEach { width ->
            assertEquals("width $width", 2, columnCount(width, isLandscape = false))
        }
    }

    @Test
    fun `landscape columns follow the width`() {
        assertEquals(3, columnCount(560, isLandscape = true))
        assertEquals(4, columnCount(805, isLandscape = true))
        assertEquals(3, columnCount(654, isLandscape = true))
        assertEquals(5, columnCount(918, isLandscape = true))
    }

    @Test
    fun `landscape never goes below three columns`() {
        assertEquals(3, columnCount(300, isLandscape = true))
        assertEquals(3, columnCount(400, isLandscape = true))
    }

    @Test
    fun `a fourth column appears exactly when four minimum width cards fit`() {
        // 4 * 150 card + 3 * 16 gaps + 2 * 16 edges = 680.
        assertEquals(4, columnCount(680, isLandscape = true))
        assertEquals(3, columnCount(679, isLandscape = true))
    }

    @Test
    fun `cells add up to the available width minus the gaps`() {
        for (columns in 2..6) {
            for (width in 300..1200) {
                assertEquals(
                    "$columns columns in $width",
                    width - (columns - 1) * 16,
                    cellSizes(columns, width).sum()
                )
            }
        }
    }

    @Test
    fun `the edge cells are wider by the edge padding and every card is the same width`() {
        val edgePx = 16
        for (columns in 2..6) {
            for (width in 300..1200) {
                val cells = cellSizes(columns, width)
                val visible = cells.mapIndexed { index, cell ->
                    when (index) {
                        0, columns - 1 -> cell - edgePx
                        else -> cell
                    }
                }
                assertTrue(
                    "$columns columns in $width: $visible",
                    visible.max() - visible.min() <= 1
                )
            }
        }
    }

    @Test
    fun `two columns at 360 reproduce the old fixed grid`() {
        // 156 dp cards after the 16 dp edge padding.
        assertEquals(listOf(172, 172), cellSizes(2, 360))
    }

    @Test
    fun `leftover pixels are handed out one per cell from the start`() {
        // 3 columns in 561: cards share 561 - 32 - 32 = 497 = 3 * 165 + 2.
        val cells = cellSizes(3, 561)
        assertEquals(listOf(165 + 1 + 16, 165 + 1, 165 + 16), cells)
        assertEquals(561 - 2 * 16, cells.sum())
    }
}
