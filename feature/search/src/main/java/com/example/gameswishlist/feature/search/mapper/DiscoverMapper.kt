package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.RecommendedShelf
import com.example.gameswishlist.core.ui.mapper.toGameItem
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.DiscoverContentState
import com.example.gameswishlist.feature.search.model.RecommendedShelfUiModel

/**
 * Splits the top anticipated pick into its own hero slot instead of also heading the "Most
 * anticipated" shelf, per the Discover-feed hero design. [DiscoverContentState.Content.upcoming]
 * excludes it, so the composable renders both fields as given instead of re-deriving the split.
 */
internal fun DiscoverFeed.toDiscoverContentState(): DiscoverContentState.Content {
    val hero = upcoming.firstOrNull()
    return DiscoverContentState.Content(
        popular = popular.toGameItemList(),
        upcoming = upcoming.drop(1).toGameItemList(),
        hero = hero?.toGameItem(),
        recommended = recommended?.toUiModel()
    )
}

/**
 * The genre name is a data-source value, so it goes into the resource as a plain argument — the
 * sentence around it is what gets localised.
 */
private fun RecommendedShelf.toUiModel(): RecommendedShelfUiModel = RecommendedShelfUiModel(
    title = UiText.StringResource(R.string.discover_because_you_like, genre.name),
    games = games.toGameItemList()
)
