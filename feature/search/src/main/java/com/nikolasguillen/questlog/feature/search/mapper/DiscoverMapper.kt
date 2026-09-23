package com.nikolasguillen.questlog.feature.search.mapper

import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.RecommendedShelf
import com.nikolasguillen.questlog.core.model.ShelfReason
import com.nikolasguillen.questlog.core.ui.mapper.toGameItem
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.R
import com.nikolasguillen.questlog.feature.search.model.DiscoverContentState
import com.nikolasguillen.questlog.feature.search.model.RecommendedShelfUiModel

/**
 * Splits the top anticipated pick into its own hero slot instead of also heading the "Most
 * anticipated" shelf, per the Discover-feed hero design. [DiscoverContentState.Content.upcoming]
 * excludes it, so the composable renders both fields as given instead of re-deriving the split.
 */
internal fun DiscoverFeed.toDiscoverContentState(savedIds: Set<Int>): DiscoverContentState.Content {
    val hero = upcoming.firstOrNull()
    return DiscoverContentState.Content(
        popular = popular.toGameItemList(savedIds),
        upcoming = upcoming.drop(1).toGameItemList(savedIds),
        hero = hero?.toGameItem(isSaved = hero.id in savedIds),
        recommended = recommended.map { it.toUiModel(savedIds) },
        isStale = hasStaleRecommendations
        // isRefreshing is not sourced from the feed: it tracks the window between the refresh tap and
        // this emission, which the ViewModel owns and this emission always closes out to false.
    )
}

/**
 * The genre or studio name is a data-source value, so it goes into the resource as a plain argument —
 * the sentence around it is what gets localised.
 */
private fun RecommendedShelf.toUiModel(savedIds: Set<Int>): RecommendedShelfUiModel = RecommendedShelfUiModel(
    // Bound to a local val: a property from another module cannot smart-cast in place.
    title = when (val shelfReason = reason) {
        is ShelfReason.ByGenre -> UiText.StringResource(R.string.discover_because_you_like, shelfReason.genre.name)
        is ShelfReason.ByDeveloper -> UiText.StringResource(R.string.discover_more_from, shelfReason.developer.name)
    },
    games = games.toGameItemList(savedIds)
)
