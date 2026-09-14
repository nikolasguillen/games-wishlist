package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RecommendedShelf
import com.example.gameswishlist.core.model.TasteProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Saved games below which the taste profile is not trusted to recommend anything. A profile built from
 * one or two games says more about what the user happened to try first than about what they like, and a
 * wrong recommendation the user cannot explain is worse than no shelf at all.
 */
private const val MIN_SAMPLE_SIZE = 3

/**
 * How many personalised shelves the feed builds at most, one per genre. Each one is its own network
 * call on a screen the user opens constantly, so this is the cap on that cost, not a design ideal.
 */
private const val MAX_RECOMMENDED_SHELVES = 2

/** How many games a personalized shelf shows once saved games and duplicates are stripped. */
private const val RECOMMENDED_SHELF_SIZE = 20

/**
 * Below this a shelf is dropped rather than rendered half-empty: a genre row holding two covers next
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
 * them, up to [MAX_RECOMMENDED_SHELVES] personalized shelves, one per genre the taste profile leans
 * towards, strongest first.
 *
 * The shelves come from independent network calls, fired concurrently. The two generic ones must both
 * succeed — a feed missing half its content with no error reads as a bug, so a single failure fails the
 * whole feed. The personalized shelves are the exception: each is additive and fails on its own, so one
 * failing degrades to fewer shelves and leaves the rest of the feed intact rather than blanking the
 * screen.
 *
 * Everything is narrowed to the platforms the user picked in Settings, and the feed re-emits whenever
 * that selection changes — the picker is one tap from this screen, so a feed that ignored it until the
 * next process start would make the setting look broken.
 *
 * The taste profile is *not* re-fetched the same way, on purpose: it is derived from the saved games,
 * which change every time the user touches a status, a priority or a list, and re-running several network
 * calls on each of those would be far more traffic than the shelves are worth. What the profile actually
 * changes for this use case is much narrower than the profile itself — see [recommendedGenreIds] — and
 * that narrower signal is cheap to keep live. So the feed does not silently go stale: once the genres it
 * would recommend today no longer match the ones [DiscoverFeed] was built from, the result is flagged
 * via [DiscoverFeed.hasStaleRecommendations] rather than refetched, leaving the decision to ask the
 * network again to the caller — see [refresh].
 */
class GetDiscoverFeedUseCase @Inject constructor(
    private val repository: GameRepository,
    private val getSelectedPlatformIds: GetSelectedPlatformIdsUseCase,
    private val getTasteProfile: GetTasteProfileUseCase
) {
    /**
     * @param refresh an external trigger to reload the feed on demand — e.g. the user tapping a "show
     * updated recommendations" prompt raised when [DiscoverFeed.hasStaleRecommendations] is true. Emits
     * nothing of its own accord in production; tests can use it to force a reload.
     * @return the feed for the current platform selection, re-emitting on every change to it or to
     * [refresh]. A selection changed mid-fetch cancels that fetch: its result describes platforms the
     * user has already moved on from.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(refresh: Flow<Unit> = emptyFlow()): Flow<AppResult<DiscoverFeed>> =
        combine(
            getSelectedPlatformIds().distinctUntilChanged(),
            refresh.onStart { emit(Unit) }
        ) { platformIds, _ -> platformIds }
            .flatMapLatest { platformIds ->
                // Captured once, before the fetch: both what the personalised shelves are built from and
                // the baseline the live genres are compared against afterwards to flag the feed stale.
                // Order matters here, not just membership -- swapping which genre leads still changes
                // which shelf the user sees first, so it counts as going stale too.
                val builtFrom = recommendedGenreIds().first()
                val feed = loadFeed(platformIds, builtFrom)

                recommendedGenreIds().distinctUntilChanged().map { currentGenreIds ->
                    feed.map { it.copy(hasStaleRecommendations = currentGenreIds != builtFrom) }
                }
            }

    private suspend fun loadFeed(platformIds: Set<Int>, genreIds: List<Int>): AppResult<DiscoverFeed> =
        coroutineScope {
            val popular = async { repository.getPopularGames(platformIds) }
            val upcoming = async { repository.getUpcomingGames(platformIds) }
            val recommended = genreIds.map { genreId -> async { loadRecommendedShelf(genreId, platformIds) } }
            val savedIds = async { repository.getSavedGames().first().mapTo(mutableSetOf()) { it.id } }

            val shelves = recommended.awaitAll().filterNotNull()
            val alreadySaved = savedIds.await()

            popular.await().zip(upcoming.await()) { popularGames, upcomingGames ->
                DiscoverFeed(
                    popular = popularGames,
                    upcoming = upcomingGames,
                    recommended = shelves.pruned(alreadySaved + popularGames.ids() + upcomingGames.ids())
                )
            }
        }

    /**
     * The [MAX_RECOMMENDED_SHELVES] strongest positive genres in the taste profile, strongest first, or
     * an empty list when the profile cannot earn a shelf yet. This is the whole of what
     * [loadRecommendedShelf] reads from [TasteProfile], which is what makes it the only profile change
     * worth reacting to: comparing it against the genres a feed was built from is what flags
     * [DiscoverFeed.hasStaleRecommendations] without a network call, and asking for a candidate pool of
     * each one is the only network cost the personalised shelves add.
     *
     * Negative weights are excluded outright — those are genres the user has actively dropped.
     *
     * Developers are a better predictor than genres per [TasteProfile], but not yet a usable one here:
     * weights are normalized within their own map, so the top developer always scores 1.0 whether it
     * was inferred from six saved games or one, and the profile carries no count to tell those apart.
     */
    private fun recommendedGenreIds(): Flow<List<Int>> = getTasteProfile().map { profile ->
        if (profile.sampleSize < MIN_SAMPLE_SIZE) return@map emptyList()
        profile.genreWeights.filterValues { it > 0.0 }
            .entries
            .sortedByDescending { it.value }
            .take(MAX_RECOMMENDED_SHELVES)
            .map { it.key }
    }

    private suspend fun loadRecommendedShelf(genreId: Int, platformIds: Set<Int>): RecommendedShelf? {
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
     * Prunes every shelf against what the user already saved, what the generic shelves are showing, and
     * every stronger shelf that came before it in [this] -- a game that qualifies for two genres is kept
     * once, in the shelf for the genre it matches most strongly, rather than recommended twice for two
     * different reasons in the same feed.
     */
    private fun List<RecommendedShelf>.pruned(excludedIds: Set<Int>): List<RecommendedShelf> {
        var excluded = excludedIds
        val result = mutableListOf<RecommendedShelf>()
        for (shelf in this) {
            val prunedShelf = shelf.pruned(excluded) ?: continue
            result += prunedShelf
            excluded = excluded + prunedShelf.games.ids()
        }
        return result
    }

    /**
     * Drops what the user already saved and whatever the generic shelves (or a stronger personalised
     * shelf) are already showing in the same feed, then caps the rest. Recommending a game that sits two
     * rows above, or one already in the user's library, is the fastest way to make the whole feed look
     * untrustworthy.
     */
    private fun RecommendedShelf.pruned(excludedIds: Set<Int>): RecommendedShelf? {
        val remaining = games
            .filterNot { it.id in excludedIds }
            .take(RECOMMENDED_SHELF_SIZE)
        return if (remaining.size < MIN_RECOMMENDED_SHELF_SIZE) null else copy(games = remaining)
    }
}
