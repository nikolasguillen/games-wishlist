package com.example.gameswishlist.core.domain.radar

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RadarEntry
import com.example.gameswishlist.core.model.RadarTimelineSection
import com.example.gameswishlist.core.model.ReleaseBucket
import com.example.gameswishlist.core.model.ReleaseDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject

/**
 * Builds the Radar timeline: every saved game with a release date, grouped into non-empty
 * [RadarTimelineSection]s and sorted ascending by date within a bucket, with two exceptions:
 * [ReleaseBucket.TBA] sorts alphabetically instead, since it has no date to sort by, and
 * [ReleaseBucket.RECENTLY_RELEASED] sorts descending, so the most recently released game leads.
 */
class GetRadarTimelineUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val getSelectedPlatformIdsUseCase: GetSelectedPlatformIdsUseCase
) {
    operator fun invoke(): Flow<List<RadarTimelineSection>> {
        return combine(
            gameRepository.getSavedGames(),
            getSelectedPlatformIdsUseCase()
        ) { games, ownedPlatformIds ->
            buildTimeline(games, ownedPlatformIds, Clock.System.now())
        }
    }

    private fun buildTimeline(
        games: List<Game>,
        ownedPlatformIds: Set<Int>,
        now: Instant
    ): List<RadarTimelineSection> {
        val bucketed = games.flatMap { game ->
            game.resolveReleaseDates(ownedPlatformIds).mapNotNull { releaseDate ->
                val bucket = resolveBucket(releaseDate.date, releaseDate.precision, now) ?: return@mapNotNull null
                bucket to RadarEntry(game, releaseDate)
            }
        }

        return ReleaseBucket.entries.mapNotNull { bucket ->
            val entries = bucketed.filter { it.first == bucket }.map { it.second }
            if (entries.isEmpty()) return@mapNotNull null
            val sortedEntries = when (bucket) {
                ReleaseBucket.TBA -> entries.sortedBy { it.game.name }
                ReleaseBucket.RECENTLY_RELEASED -> entries.sortedByDescending { it.releaseDate.date }
                else -> entries.sortedBy { it.releaseDate.date }
            }
            RadarTimelineSection(bucket, sortedEntries)
        }
    }
}

/**
 * Per the multi-platform date resolution decision: one release date per platform the user owns that the
 * game has a date for (earliest region date within each owned platform); if there's no selection or no
 * match, falls back to a single entry for the earliest date across all the game's platforms. An empty list
 * means the game has no release date data at all.
 */
private fun Game.resolveReleaseDates(ownedPlatformIds: Set<Int>): List<ReleaseDate> {
    val ownedDates = releaseDates.filter { it.platformId in ownedPlatformIds }
    if (ownedDates.isNotEmpty()) {
        return ownedDates
            .groupBy { it.platformId }
            .mapNotNull { (_, datesForPlatform) -> datesForPlatform.minByOrNull { it.date ?: Long.MAX_VALUE } }
    }
    val fallback = releaseDates.minByOrNull { it.date ?: Long.MAX_VALUE } ?: return emptyList()
    return listOf(fallback)
}
