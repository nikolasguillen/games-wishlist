package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Use case to load the cold-start Discover feed — the generic popular + upcoming shelves shown before a
 * `TasteProfile` is available. Personalised ranking layers on top later; this is the path the roadmap
 * calls the first shippable milestone.
 *
 * The two shelves come from independent network calls, fired concurrently. Both must succeed: a feed
 * missing half its content with no error reads as a bug, so a single failure fails the whole feed.
 */
class GetDiscoverFeedUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(): AppResult<DiscoverFeed> = coroutineScope {
        val popular = async { repository.getPopularGames() }
        val upcoming = async { repository.getUpcomingGames() }
        combine(popular.await(), upcoming.await())
    }

    private fun combine(
        popular: AppResult<List<Game>>,
        upcoming: AppResult<List<Game>>
    ): AppResult<DiscoverFeed> = when {
        popular is AppResult.Success && upcoming is AppResult.Success ->
            AppResult.success(DiscoverFeed(popular = popular.data, upcoming = upcoming.data))

        popular is AppResult.Failure -> popular
        else -> upcoming as AppResult.Failure
    }
}
