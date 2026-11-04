package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.max

// The layout dimensions of the wide Discover feed. They are not spacing tokens: like MIN_CARD_WIDTH in
// SearchResultGridCells.kt and HERO_MAX_HEIGHT in DiscoverHero.kt they live next to the code that uses them.
// Gaps and paddings still come from MaterialTheme.spacing.

/** The narrowest a shelf card gets; the desktop mockup shows ~167dp cards, six of them up to ~1080dp. */
internal val WIDE_SHELF_CARD_MIN_WIDTH = 160.dp

/** A shelf card is a portrait cover, as the grid of the expanded search home. */
internal const val WIDE_SHELF_CARD_ASPECT_RATIO = 3f / 4f

/** The share of the window's height the hero banner takes before the floor, the ceiling and the width cap. */
internal const val WIDE_HERO_HEIGHT_FRACTION = 0.45f

/** The hero is never shorter than this, so it stays clearly taller than the phone hero (240dp) at any height. */
internal val WIDE_HERO_MIN_HEIGHT = 280.dp

/** The hero is never taller than this, however tall the window is. */
internal val WIDE_HERO_MAX_HEIGHT = 560.dp

/** The hero's height never exceeds this share of its own width, so a half-width pane does not get a tower. */
internal const val WIDE_HERO_MAX_ASPECT = 0.8f

/** Below this banner width the hero's two actions move from the end of the title row to under the title. */
internal val WIDE_HERO_STACKED_BELOW = 600.dp

/**
 * How many cards a wide shelf row holds.
 *
 * As many [WIDE_SHELF_CARD_MIN_WIDTH] cards as the row's own [availableWidth] holds, between the [edge] paddings and
 * with [spacing] between them, and never fewer than one. The row measures its own width (not the window's), so a
 * narrow pane gets fewer cards instead of cut-off ones.
 */
internal fun wideShelfColumnCount(availableWidth: Dp, spacing: Dp, edge: Dp): Int {
    val fitting = floor((availableWidth - edge * 2 + spacing) / (WIDE_SHELF_CARD_MIN_WIDTH + spacing)).toInt()
    return max(1, fitting)
}

/**
 * Whether a wide shelf shows its "See all" link: when the shelf has more games than it carries, or when this
 * width hides some of the games it does carry ([gameCount] above [columns]).
 */
internal fun wideShelfShowsSeeAll(hasMore: Boolean, gameCount: Int, columns: Int): Boolean =
    hasMore || gameCount > columns

/**
 * The hero banner's height: [WIDE_HERO_HEIGHT_FRACTION] of the [windowHeight], kept between
 * [WIDE_HERO_MIN_HEIGHT] and [WIDE_HERO_MAX_HEIGHT], and capped at [WIDE_HERO_MAX_ASPECT] of the banner's own
 * [availableWidth]. The width cap wins over the floor.
 */
internal fun wideHeroHeight(windowHeight: Dp, availableWidth: Dp): Dp =
    minOf(
        (windowHeight * WIDE_HERO_HEIGHT_FRACTION).coerceIn(WIDE_HERO_MIN_HEIGHT, WIDE_HERO_MAX_HEIGHT),
        availableWidth * WIDE_HERO_MAX_ASPECT
    )
