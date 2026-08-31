package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case to load the cold-start Discover feed — the generic popular + upcoming shelves shown before a
 * `TasteProfile` is available. Personalised ranking layers on top later; this is the path the roadmap
 * calls the first shippable milestone.
 *
 * The two shelves come from independent network calls, fired concurrently. Both must succeed: a feed
 * missing half its content with no error reads as a bug, so a single failure fails the whole feed.
 *
 * Both shelves are narrowed to the platforms the user picked in Settings, read once per load rather
 * than observed: the feed is a snapshot the caller re-requests, not a live query.
 */
class GetDiscoverFeedUseCase @Inject constructor(
    private val repository: GameRepository,
    private val getSelectedPlatformIds: GetSelectedPlatformIdsUseCase
) {
    suspend operator fun invoke(): AppResult<DiscoverFeed> = coroutineScope {
        val platformIds = getSelectedPlatformIds().first()
        val popular = async { repository.getPopularGames(platformIds) }
        val upcoming = async { repository.getUpcomingGames(platformIds) }
        popular.await().zip(upcoming.await()) { p, u ->
            DiscoverFeed(popular = p, upcoming = u)
        }
    }
}
