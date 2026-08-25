package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

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
 * Use case to derive the user's [TasteProfile] from the games they have saved.
 *
 * Reads straight from local storage and re-emits whenever the library changes. A user who has saved
 * nothing gets [TasteProfile.EMPTY], which callers should read as "fall back to generic content"
 * rather than "this user likes nothing".
 */
class GetTasteProfileUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * @return A flow of the current profile, recomputed on every change to the saved games.
     */
    operator fun invoke(): Flow<TasteProfile> {
        return repository.getSavedGames().map { games ->
            if (games.isEmpty()) TasteProfile.EMPTY else buildProfile(games)
        }
    }

    private fun buildProfile(games: List<Game>): TasteProfile {
        val recentlyViewedIds = games
            .filter { it.lastViewedAt != null }
            .sortedByDescending { it.lastViewedAt }
            .take(RECENTLY_VIEWED_BONUS_COUNT)
            .map { it.id }
            .toSet()

        val genreScores = mutableMapOf<Int, Double>()
        val developerScores = mutableMapOf<Int, Double>()

        games.forEach { game ->
            val weight = gameWeight(game, isRecentlyViewed = game.id in recentlyViewedIds)
            // Publishers are deliberately not a signal: a publisher's catalogue spans genres the
            // user never chose, so it predicts far worse than the studio that made the game.
            game.genres.forEach { genreScores.merge(it.id, weight, Double::plus) }
            game.developers.forEach { developerScores.merge(it.id, weight, Double::plus) }
        }

        return TasteProfile(
            genreWeights = genreScores.normalised(),
            developerWeights = developerScores.normalised(),
            sampleSize = games.size
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
     */
    private fun Map<Int, Double>.normalised(): Map<Int, Double> {
        val peak = values.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0
        if (peak == 0.0) return emptyMap()
        return mapValues { (_, score) -> score / peak }
    }
}
