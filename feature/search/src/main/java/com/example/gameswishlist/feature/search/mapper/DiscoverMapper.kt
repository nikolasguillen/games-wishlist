package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.ui.mapper.toGameItem
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.feature.search.model.SearchContentState

/**
 * Splits the top anticipated pick into its own hero slot instead of also heading the "Most
 * anticipated" shelf, per the Discover-feed hero design. [SearchContentState.Discover.upcoming]
 * excludes it, so the composable renders both fields as given instead of re-deriving the split.
 */
internal fun DiscoverFeed.toDiscoverContentState(): SearchContentState.Discover {
    val hero = upcoming.firstOrNull()
    return SearchContentState.Discover(
        popular = popular.toGameItemList(),
        upcoming = upcoming.drop(1).toGameItemList(),
        hero = hero?.toGameItem()
    )
}
