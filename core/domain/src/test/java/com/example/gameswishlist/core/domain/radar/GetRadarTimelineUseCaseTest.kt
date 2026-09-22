package com.example.gameswishlist.core.domain.radar

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.domain.usecase.discover.GetSelectedPlatformIdsUseCase
import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.ReleaseBucket
import com.example.gameswishlist.core.model.ReleaseDate
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Clock

private const val PC = 6
private const val PS5 = 167
private const val SWITCH = 130

/**
 * Covers the multi-platform date resolution decision's three branches (owned-platform match, no-selection
 * fallback, no-matching-platform fallback) and that a game's platforms all resolve to one entry. Bucket
 * boundaries themselves are [ReleaseBucketResolverTest]'s job -- fixtures here use a date far enough in
 * the future to land in [ReleaseBucket.LATER] regardless of when the test runs, so only the resolved date
 * is asserted on.
 */
class GetRadarTimelineUseCaseTest {

    private val gameRepository = mockk<GameRepository>()
    private val getSelectedPlatformIdsUseCase = mockk<GetSelectedPlatformIdsUseCase>()

    private val useCase = GetRadarTimelineUseCase(gameRepository, getSelectedPlatformIdsUseCase)

    // Comfortably beyond the "next 3 months" window no matter when this test runs.
    private val farFuture = Clock.System.now().epochSeconds + (400L * 24 * 3600)

    private fun releaseDate(platformId: Int, date: Long) =
        ReleaseDate(date = date, platformId = platformId, platformName = "Platform $platformId")

    private fun given(games: List<Game>, ownedPlatformIds: Set<Int>) {
        every { gameRepository.getSavedGames() } returns flowOf(games)
        every { getSelectedPlatformIdsUseCase() } returns flowOf(ownedPlatformIds)
    }

    @Test
    fun `resolves the owned platform's date over an earlier date on another platform`() = runTest {
        given(
            games = listOf(
                Game(
                    id = 1,
                    name = "Game",
                    releaseDates = listOf(
                        releaseDate(PS5, farFuture), // not owned, earliest
                        releaseDate(PC, farFuture + 1_000) // owned
                    )
                )
            ),
            ownedPlatformIds = setOf(PC)
        )

        val entry = useCase().first().single().entries.single()

        assertEquals(farFuture + 1_000, entry.releaseDate.date)
        assertEquals(PC, entry.releaseDate.platformId)
    }

    @Test
    fun `falls back to the earliest date across all platforms when nothing is selected`() = runTest {
        given(
            games = listOf(
                Game(
                    id = 1,
                    name = "Game",
                    releaseDates = listOf(
                        releaseDate(PC, farFuture + 1_000),
                        releaseDate(PS5, farFuture)
                    )
                )
            ),
            ownedPlatformIds = emptySet()
        )

        val entry = useCase().first().single().entries.single()

        assertEquals(farFuture, entry.releaseDate.date)
        assertEquals(PS5, entry.releaseDate.platformId)
    }

    @Test
    fun `falls back to the earliest date when the selection matches none of the game's platforms`() = runTest {
        given(
            games = listOf(
                Game(
                    id = 1,
                    name = "Game",
                    releaseDates = listOf(
                        releaseDate(PC, farFuture + 1_000),
                        releaseDate(PS5, farFuture)
                    )
                )
            ),
            ownedPlatformIds = setOf(SWITCH)
        )

        val entry = useCase().first().single().entries.single()

        assertEquals(farFuture, entry.releaseDate.date)
    }

    @Test
    fun `a game with no release date at all is dropped from the timeline`() = runTest {
        given(
            games = listOf(Game(id = 1, name = "No dates", releaseDates = emptyList())),
            ownedPlatformIds = emptySet()
        )

        assertEquals(emptyList<Any>(), useCase().first())
    }

    @Test
    fun `TBA entries sort alphabetically since there is no date to sort by`() = runTest {
        given(
            games = listOf(
                Game(
                    id = 1,
                    name = "Zelda-like",
                    releaseDates = listOf(
                        ReleaseDate(date = null, platformId = PC, platformName = "PC", precision = DatePrecision.TBD)
                    )
                ),
                Game(
                    id = 2,
                    name = "Alpha Quest",
                    releaseDates = listOf(
                        ReleaseDate(date = null, platformId = PC, platformName = "PC", precision = DatePrecision.TBD)
                    )
                )
            ),
            ownedPlatformIds = emptySet()
        )

        val section = useCase().first().single()

        assertEquals(ReleaseBucket.TBA, section.bucket)
        assertEquals(listOf("Alpha Quest", "Zelda-like"), section.entries.map { it.game.name })
    }
}
