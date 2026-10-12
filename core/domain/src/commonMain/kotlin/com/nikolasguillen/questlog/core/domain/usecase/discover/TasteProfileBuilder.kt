package com.nikolasguillen.questlog.core.domain.usecase.discover

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Priority
import com.nikolasguillen.questlog.core.model.TasteProfile
import com.nikolasguillen.questlog.core.model.TasteSignal

/**
 * How many of the most recently opened games get the recency nudge.
 *
 * The bonus is a tie-break between titles the explicit signals already scored equally, so it applies
 * to a handful of games rather than decaying across the whole library.
 */
private const val RECENTLY_VIEWED_BONUS_COUNT = 5

/** Small enough that it can only reorder near-ties, never outrank a status difference. */
private const val RECENTLY_VIEWED_BONUS = 0.1

/**
 * Derives the [TasteProfile] of a library of saved games. Shared by every use case that reads the user's
 * taste ([GetTasteProfileUseCase] for Discover, the genre chips of Search), so they all agree on what
 * "the user likes this genre" means.
 *
 * Callers short-circuit an empty library to [TasteProfile.EMPTY] themselves.
 */
internal fun List<Game>.toTasteProfile(): TasteProfile {
    val recentlyViewedIds = this
        .filter { it.lastViewedAt != null }
        .sortedByDescending { it.lastViewedAt }
        .take(RECENTLY_VIEWED_BONUS_COUNT)
        .map { it.id }
        .toSet()

    val genreScores = mutableMapOf<Int, Double>()
    val genreCounts = mutableMapOf<Int, Int>()
    val developerScores = mutableMapOf<Int, Double>()
    val developerCounts = mutableMapOf<Int, Int>()

    forEach { game ->
        val weight = gameWeight(game, isRecentlyViewed = game.id in recentlyViewedIds)
        // Counts only accumulate for a positive contribution: a dropped game (negative weight)
        // is a rejection, not a recurrence, and must not make a genre or studio look more
        // frequent than it actually is to the user.
        val countsPositively = weight > 0.0
        // Publishers are deliberately not a signal: a publisher's catalogue spans genres the
        // user never chose, so it predicts far worse than the studio that made the game.
        game.genres.forEach { genre ->
            genreScores.addScore(genre.id, weight)
            if (countsPositively) genreCounts.addCount(genre.id)
        }
        game.developers.forEach { developer ->
            developerScores.addScore(developer.id, weight)
            if (countsPositively) developerCounts.addCount(developer.id)
        }
    }

    return TasteProfile(
        genres = genreScores.toSignals(genreCounts),
        developers = developerScores.toSignals(developerCounts),
        sampleSize = size
    )
}

/**
 * The contribution one game makes to every genre and developer it carries.
 *
 * Status leads because it is the only signal that says what the user did with the game rather
 * than what they intended, priority scales it because it is a weight the user set by hand, and
 * recency only nudges. A dropped game contributes a *negative* weight — a rejection is as
 * informative as an endorsement, and without it a genre the user keeps bouncing off would keep
 * climbing on the strength of having been saved at all.
 */
private fun gameWeight(game: Game, isRecentlyViewed: Boolean): Double {
    val statusWeight = when (game.status) {
        GameStatus.COMPLETED -> 1.0
        GameStatus.PLAYING -> 1.0
        GameStatus.BOUGHT -> 0.6
        // Intent, not taste: the user wants it, but has not lived with it yet.
        GameStatus.WANT_TO_BUY -> 0.3
        GameStatus.DROPPED -> -1.0
        null -> 0.4
    }

    val priorityMultiplier = when (game.priority) {
        Priority.HIGH -> 1.3
        Priority.MEDIUM -> 1.0
        Priority.LOW -> 0.8
        null -> 1.0
    }

    val bonus = if (isRecentlyViewed) RECENTLY_VIEWED_BONUS else 0.0
    return statusWeight * priorityMultiplier + bonus
}

/**
 * Rescales to `-1.0..1.0` against the largest magnitude in the map, so the profile of a user
 * with forty saved games is comparable to one with four and a caller can apply a fixed
 * threshold. Dividing by the largest *absolute* value keeps the sign of negative weights.
 *
 * Pairs the rescaled weight with the raw positive-contribution count from [counts] — the count is
 * never rescaled, since it is the thing a caller reads precisely because a normalised weight alone
 * cannot tell a pattern from a coincidence (see [TasteSignal.count]).
 */
private fun Map<Int, Double>.toSignals(counts: Map<Int, Int>): Map<Int, TasteSignal> {
    val peak = values.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0
    if (peak == 0.0) return emptyMap()
    return mapValues { (id, score) -> TasteSignal(weight = score / peak, count = counts[id] ?: 0) }
}

private fun MutableMap<Int, Double>.addScore(id: Int, weight: Double) {
    this[id] = (this[id] ?: 0.0) + weight
}

private fun MutableMap<Int, Int>.addCount(id: Int) {
    this[id] = (this[id] ?: 0) + 1
}
