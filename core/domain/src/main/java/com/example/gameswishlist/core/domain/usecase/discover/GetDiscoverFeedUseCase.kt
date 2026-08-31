package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RecommendedShelf
import com.example.gameswishlist.core.model.TasteProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Saved games below which the taste profile is not trusted to recommend anything. A profile built from
 * one or two games says more about what the user happened to try first than about what they like, and a
 * wrong recommendation the user cannot explain is worse than no shelf at all.
 */
private const val MIN_SAMPLE_SIZE = 3

/** How many games the personalised shelf shows once saved games and duplicates are stripped. */
private const val RECOMMENDED_SHELF_SIZE = 20

/**
 * Below this the shelf is dropped rather than rendered half-empty: a genre row holding two covers next
 * to two full generic shelves reads as a loading bug.
 */
private const val MIN_RECOMMENDED_SHELF_SIZE = 4

/**
 * How many ratings a game needs before its score is taken at face value, used as the weight of the
 * prior in [weightedRating].
 *
 * This is what replaces excluding thinly-rated games outright. A hard floor would drop every niche and
 * newly released title in the genre — exactly the games a "because you like X" shelf exists to surface —
 * while doing nothing about a mediocre game that happens to clear it. Pulling a score towards the mean
 * in proportion to how few people voted for it handles both, and lets a genuinely loved game with a
 * handful of ratings still outrank a merely decent famous one.
 */
private const val RATING_CONFIDENCE_THRESHOLD = 20.0

/**
 * The score a game is assumed to deserve before its own ratings say otherwise, on IGDB's 0..100 scale.
 *
 * Together with [RATING_CONFIDENCE_THRESHOLD] this is the shelf's main tuning knob: raise it and thinly
 * rated games climb, lower it and the shelf fills with established titles. Both numbers are reasoned
 * guesses about IGDB's rating distribution and have not been checked against a real pool.
 */
private const val NEUTRAL_RATING = 75.0

/**
 * Use case to load the Discover feed: the two generic shelves plus, when the user's library supports
 * one, a personalised shelf built from the strongest genre in their [TasteProfile].
 *
 * The shelves come from independent network calls, fired concurrently. The two generic ones must both
 * succeed — a feed missing half its content with no error reads as a bug, so a single failure fails the
 * whole feed. The personalised shelf is the exception: it is additive, so a failure there degrades to no
 * shelf and leaves a complete generic feed rather than blanking the screen.
 *
 * Everything is narrowed to the platforms the user picked in Settings, read once per load rather than
 * observed: the feed is a snapshot the caller re-requests, not a live query.
 */
class GetDiscoverFeedUseCase @Inject constructor(
    private val repository: GameRepository,
    private val getSelectedPlatformIds: GetSelectedPlatformIdsUseCase,
    private val getTasteProfile: GetTasteProfileUseCase
) {
    suspend operator fun invoke(): AppResult<DiscoverFeed> = coroutineScope {
        val platformIds = getSelectedPlatformIds().first()
        val profile = getTasteProfile().first()

        val popular = async { repository.getPopularGames(platformIds) }
        val upcoming = async { repository.getUpcomingGames(platformIds) }
        val recommended = async { loadRecommendedShelf(profile, platformIds) }
        val savedIds = async { repository.getSavedGames().first().mapTo(mutableSetOf()) { it.id } }

        val shelf = recommended.await()
        val alreadySaved = savedIds.await()

        popular.await().zip(upcoming.await()) { popularGames, upcomingGames ->
            DiscoverFeed(
                popular = popularGames,
                upcoming = upcomingGames,
                recommended = shelf?.pruned(alreadySaved + popularGames.ids() + upcomingGames.ids())
            )
        }
    }

    /**
     * Picks the single strongest genre and asks for a candidate pool of it.
     *
     * One shelf, not one per signal: every extra shelf is another network call on a screen the user
     * opens constantly, and a feed that recommends five things at once explains none of them well.
     * Negative weights are excluded outright — those are genres the user has actively dropped.
     *
     * Developers are a better predictor than genres per [TasteProfile], but not yet a usable one here:
     * weights are normalised within their own map, so the top developer always scores 1.0 whether it
     * was inferred from six saved games or one, and the profile carries no count to tell those apart.
     */
    private suspend fun loadRecommendedShelf(
        profile: TasteProfile,
        platformIds: Set<Int>
    ): RecommendedShelf? {
        if (profile.sampleSize < MIN_SAMPLE_SIZE) return null
        val genreId = profile.genreWeights
            .filterValues { it > 0.0 }
            .maxByOrNull { it.value }
            ?.key
            ?: return null

        val result = repository.getGamesByGenre(genreId, platformIds)
        val games = (result as? AppResult.Success)?.data ?: return null
        // The profile stores ids, so the name has to come off the results themselves. No match means
        // nothing came back to name the shelf after, which is the same as having no shelf.
        val genre = games.firstNotNullOfOrNull { game ->
            game.genres.firstOrNull { it.id == genreId }
        } ?: return null

        return RecommendedShelf(genre = genre, games = games.rankedByWeightedRating())
    }

    /**
     * The fine, local half of the recommendation: IGDB can sort by raw score but not by a score
     * weighted for confidence, so the pool arrives led by whatever carries a high average across very
     * few votes.
     *
     * Bayesian average of the game's own score against [NEUTRAL_RATING], weighted by its rating count
     * against [RATING_CONFIDENCE_THRESHOLD]. A game with no ratings scores exactly the prior, which
     * puts it mid-pool rather than first or last — unknown is not the same as bad.
     *
     * The prior is deliberately a constant and not the pool's own mean. The pool arrives sorted by raw
     * rating, so its mean is the mean of the genre's *best-scored* games, not of the genre: using it
     * would shrink every score towards ~90 and cancel out the correction this exists to apply.
     */
    private fun List<Game>.rankedByWeightedRating(): List<Game> = sortedByDescending { game ->
        val count = game.ratingCount
        (count * game.rating + RATING_CONFIDENCE_THRESHOLD * NEUTRAL_RATING) /
            (count + RATING_CONFIDENCE_THRESHOLD)
    }

    private fun List<Game>.ids(): Set<Int> = mapTo(mutableSetOf()) { it.id }

    /**
     * Drops what the user already saved and whatever the generic shelves are showing in the same feed,
     * then caps the rest. Recommending a game that sits two rows above, or one already in the user's
     * library, is the fastest way to make the whole feed look untrustworthy.
     */
    private fun RecommendedShelf.pruned(excludedIds: Set<Int>): RecommendedShelf? {
        val remaining = games
            .filterNot { it.id in excludedIds }
            .take(RECOMMENDED_SHELF_SIZE)
        return if (remaining.size < MIN_RECOMMENDED_SHELF_SIZE) null else copy(games = remaining)
    }
}
