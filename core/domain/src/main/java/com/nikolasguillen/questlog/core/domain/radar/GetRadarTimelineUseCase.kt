package com.nikolasguillen.questlog.core.domain.radar

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RadarEntry
import com.nikolasguillen.questlog.core.model.RadarTimelineSection
import com.nikolasguillen.questlog.core.model.ReleaseBucket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Builds the Radar timeline: every saved game with a release date, grouped into non-empty
 * [RadarTimelineSection]s and sorted ascending by date within a bucket, with two exceptions:
 * [ReleaseBucket.TBA] sorts alphabetically instead, since it has no date to sort by, and
 * [ReleaseBucket.RECENTLY_RELEASED] sorts descending, so the most recently released game leads.
 */
class GetRadarTimelineUseCase(
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
