package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

/**
 * The narrowest a results card gets: what a two-column portrait card measures on the phones the app targets. In
 * landscape the grid adds columns for as long as every card can stay at least this wide.
 */
internal val MIN_CARD_WIDTH = 150.dp

/** Landscape shows at least this many columns, even where the width would only fit fewer minimum width cards. */
private const val MIN_LANDSCAPE_COLUMNS = 3

private const val PORTRAIT_COLUMNS = 2

/**
 * How many columns the results grid has.
 *
 * Portrait is always two, whatever the width, so a portrait tablet or a split-screen half does not change. Landscape
 * fits as many [MIN_CARD_WIDTH] cards as the grid's own [availableWidth] holds, between the [edge] paddings and with
 * [spacing] between them, and never fewer than three.
 */
internal fun resultGridColumnCount(
    availableWidth: Dp,
    spacing: Dp,
    edge: Dp,
    isLandscape: Boolean
): Int {
    if (!isLandscape) return PORTRAIT_COLUMNS
    val fitting = floor((availableWidth - edge * 2 + spacing) / (MIN_CARD_WIDTH + spacing)).toInt()
    return max(MIN_LANDSCAPE_COLUMNS, fitting)
}

/**
 * Equal-width columns whose first and last cells are wider by [edgePadding].
 *
 * The cards in the first and last column pad that much on their outer side, so every visible card ends up the same
 * width at any column count, while the cells themselves reach the grid's edges: the full-span filters row keeps
 * bleeding to the screen edge instead of being inset with the cards. With two columns this is exactly the geometry of
 * `GridCells.Fixed(2)` with a card padded on its outer side.
 */
internal data class SearchResultGridCells(
    private val columnCount: Int,
    private val edgePadding: Dp
) : GridCells {

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val edge = edgePadding.roundToPx()
        val cardsWidth = (availableSize - (columnCount - 1) * spacing - 2 * edge).coerceAtLeast(0)
        val cardWidth = cardsWidth / columnCount
        // Whatever does not divide evenly goes to the first cells, one pixel each, so no pixel is lost.
        val leftover = cardsWidth % columnCount
        return List(columnCount) { column ->
            var size = cardWidth
            if (column < leftover) size += 1
            if (column == 0) size += edge
            if (column == columnCount - 1) size += edge
            size
        }
    }
}
