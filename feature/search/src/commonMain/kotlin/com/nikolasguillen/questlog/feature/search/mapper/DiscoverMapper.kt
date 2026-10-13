package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RecommendedShelf
import com.nikolasguillen.questlog.core.model.ShelfReason
import com.nikolasguillen.questlog.core.ui.mapper.toGameItem
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.DiscoverContentState
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_because_you_like
import com.nikolasguillen.questlog.feature.search.resources.discover_more_from
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month

/**
 * How many games a Discover row shows. A display cap only: the feed still fetches, caches and de-duplicates
 * the whole lane, and the shelf's grid shows all of it. It lives here and not in `UiConstants` because
 * nothing else reads it.
 */
internal const val DISCOVER_SHELF_ROW_SIZE = 6

/**
 * Splits the top anticipated pick into its own hero slot instead of also heading the "Most
 * anticipated" shelf, per the Discover-feed hero design. [DiscoverContentState.Content.upcoming]
 * excludes it, so the composable renders both fields as given instead of re-deriving the split.
 *
 * Every shelf is capped at [DISCOVER_SHELF_ROW_SIZE] games here, and says whether it holds more, so the
 * composable only renders what it is given and never counts games.
 */
internal fun DiscoverFeed.toDiscoverContentState(savedIds: Set<Int>): DiscoverContentState.Content {
    val hero = upcoming.firstOrNull()
    return DiscoverContentState.Content(
        popular = laneShelf(
            lane = DiscoverLane.POPULAR_THIS_MONTH,
            title = UiText.StringResource(Res.string.discover_popular_this_month),
            savedIds = savedIds
        ),
        upcoming = laneShelf(
            lane = DiscoverLane.MOST_ANTICIPATED,
            title = UiText.StringResource(Res.string.discover_most_anticipated),
            savedIds = savedIds
        ),
        hero = hero?.toGameItem(isSaved = hero.id in savedIds),
        recommended = recommended.mapNotNull { it.toUiModel(savedIds) },
        isStale = hasStaleRecommendations
        // isRefreshing is not sourced from the feed: it tracks the window between the refresh tap and
        // this emission, which the ViewModel owns and this emission always closes out to false.
    )
}

/**
 * The games of one Discover shelf, whole and in the shelf's own order, or `null` when the feed holds no such
 * shelf -- a refresh can drop a personalised shelf between the moment its arrow is rendered and the moment it
 * is tapped.
 *
 * This is the single definition of what a shelf holds: the capped row is built from its first
 * [DISCOVER_SHELF_ROW_SIZE] games and the grid shows all of them, so the row is always the start of the
 * grid. "Most anticipated" is the lane without its top pick, which the hero slot already shows.
 */
internal fun DiscoverFeed.shelfGames(key: DiscoverShelfKey): List<Game>? = when (key) {
    is DiscoverShelfKey.Lane -> when (key.lane) {
        DiscoverLane.MOST_ANTICIPATED -> upcoming.drop(1)
        DiscoverLane.POPULAR_THIS_MONTH -> popular
    }

    is DiscoverShelfKey.ByGenre -> recommended.firstOrNull { shelf ->
        // Bound to a local val: a property from another module cannot smart-cast in place.
        val reason = shelf.reason
        reason is ShelfReason.ByGenre && reason.genre.id == key.genreId
    }?.games

    is DiscoverShelfKey.ByDeveloper -> recommended.firstOrNull { shelf ->
        val reason = shelf.reason
        reason is ShelfReason.ByDeveloper && reason.developer.id == key.developerId
    }?.games
}

private fun DiscoverFeed.laneShelf(lane: DiscoverLane, title: UiText, savedIds: Set<Int>): DiscoverShelfUiModel? {
    val key = DiscoverShelfKey.Lane(lane)
    return shelfUiModel(key, title, shelfGames(key).orEmpty(), savedIds)
}

/**
 * The genre or studio name is a data-source value, so it goes into the resource as a plain argument —
 * the sentence around it is what gets localised. The key is the id of that same signal, so the shelf
 * can be found again by id, whatever its title says.
 */
private fun RecommendedShelf.toUiModel(savedIds: Set<Int>): DiscoverShelfUiModel? {
    // Bound to a local val: a property from another module cannot smart-cast in place.
    return when (val shelfReason = reason) {
        is ShelfReason.ByGenre -> shelfUiModel(
            key = DiscoverShelfKey.ByGenre(shelfReason.genre.id),
            title = UiText.StringResource(Res.string.discover_because_you_like, shelfReason.genre.name),
            games = games,
            savedIds = savedIds
        )

        is ShelfReason.ByDeveloper -> shelfUiModel(
            key = DiscoverShelfKey.ByDeveloper(shelfReason.developer.id),
            title = UiText.StringResource(Res.string.discover_more_from, shelfReason.developer.name),
            games = games,
            savedIds = savedIds
        )
    }
}

/** A shelf with nothing in it is no shelf: it maps to `null` rather than to an empty row under a heading. */
private fun shelfUiModel(
    key: DiscoverShelfKey,
    title: UiText,
    games: List<Game>,
    savedIds: Set<Int>
): DiscoverShelfUiModel? {
    if (games.isEmpty()) return null
    return DiscoverShelfUiModel(
        key = key,
        title = title,
        games = games.take(DISCOVER_SHELF_ROW_SIZE).toGameItemList(savedIds),
        hasMore = games.size > DISCOVER_SHELF_ROW_SIZE
    )
}
