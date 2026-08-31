package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.model.TasteProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private val PLAYING = Game(id = 1, name = "Cindergate")
private val ANTICIPATED = Game(id = 2, name = "Ashborne Reverie")

private val RPG = Genre(id = 12, name = "RPG")
private val SHOOTER = Genre(id = 5, name = "Shooter")

/** Enough saved games to clear the use case's minimum sample size. */
private const val TRUSTED_SAMPLE_SIZE = 8

private fun rpgGame(id: Int, rating: Double = 0.0, ratingCount: Int = 0) =
    Game(id = id, name = "RPG $id", rating = rating, ratingCount = ratingCount, genres = listOf(RPG))

/**
 * Covers [GetDiscoverFeedUseCase]: the two generic shelves are combined into one [DiscoverFeed] when
 * both sources succeed, a single generic failure fails the whole feed, the platforms the user picked in
 * Settings reach every shelf, and the personalised shelf appears only when the taste profile earns it —
 * ranked by confidence-weighted rating, then pruned of saved games and of duplicates from the generic
 * shelves.
 */
class GetDiscoverFeedUseCaseTest {

    private val repository = mockk<GameRepository>()
    private val getSelectedPlatformIds = mockk<GetSelectedPlatformIdsUseCase>()
    private val getTasteProfile = mockk<GetTasteProfileUseCase>()

    private val useCase = GetDiscoverFeedUseCase(repository, getSelectedPlatformIds, getTasteProfile)

    @Before
    fun setUp() {
        every { getSelectedPlatformIds() } returns flowOf(emptySet())
        // Cold start by default: the personalised shelf stays out of the way unless a test asks for it.
        every { getTasteProfile() } returns flowOf(TasteProfile.EMPTY)
        every { repository.getSavedGames() } returns flowOf(emptyList())
        coEvery { repository.getPopularGames(any()) } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.success(listOf(ANTICIPATED))
    }

    /** A profile the use case will trust, leaning towards [RPG] and away from [SHOOTER]. */
    private fun rpgProfile(sampleSize: Int = TRUSTED_SAMPLE_SIZE) = TasteProfile(
        genreWeights = mapOf(RPG.id to 1.0, SHOOTER.id to -0.4),
        sampleSize = sampleSize
    )

    private fun feedOrNull(result: AppResult<DiscoverFeed>) = (result as? AppResult.Success)?.data

    @Test
    fun `combines both shelves when both sources succeed`() = runTest {
        val result = useCase()

        assertEquals(
            AppResult.success(DiscoverFeed(popular = listOf(PLAYING), upcoming = listOf(ANTICIPATED))),
            result
        )
    }

    @Test
    fun `fails the feed when the popular source fails`() = runTest {
        coEvery { repository.getPopularGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }

    @Test
    fun `fails the feed when the upcoming source fails`() = runTest {
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }

    @Test
    fun `passes the selected platforms to every shelf`() = runTest {
        val selection = setOf(48, 130)
        every { getSelectedPlatformIds() } returns flowOf(selection)
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(any(), any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        useCase()

        coVerify { repository.getPopularGames(selection) }
        coVerify { repository.getUpcomingGames(selection) }
        coVerify { repository.getGamesByGenre(RPG.id, selection) }
    }

    @Test
    fun `builds the personalised shelf from the strongest positive genre`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        val shelf = feedOrNull(useCase())?.recommended

        assertEquals(RPG, shelf?.genre)
        assertEquals((10..20).toList(), shelf?.games?.map { it.id })
    }

    @Test
    fun `weights ratings by confidence instead of excluding thinly rated games`() = runTest {
        val twoVotesPerfect = rpgGame(id = 30, rating = 100.0, ratingCount = 2)
        val wellRatedNiche = rpgGame(id = 31, rating = 95.0, ratingCount = 40)
        val establishedGreat = rpgGame(id = 32, rating = 92.0, ratingCount = 800)
        val popularButMediocre = rpgGame(id = 33, rating = 70.0, ratingCount = 500)
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns AppResult.success(
            listOf(twoVotesPerfect, wellRatedNiche, establishedGreat, popularButMediocre)
        )

        val shelf = feedOrNull(useCase())?.recommended

        // The two-vote 100 is pushed below the scores people actually voted on, but it is still on the
        // shelf and still ahead of a mediocre game -- a hard rating-count floor would have dropped it,
        // along with every niche and newly released game in the genre.
        assertEquals(listOf(32, 31, 30, 33), shelf?.games?.map { it.id })
    }

    @Test
    fun `never recommends a genre the user has dropped`() = runTest {
        // Only SHOOTER has a weight, and it is negative: a rejection is not a recommendation.
        every { getTasteProfile() } returns
            flowOf(TasteProfile(genreWeights = mapOf(SHOOTER.id to -1.0), sampleSize = TRUSTED_SAMPLE_SIZE))

        assertNull(feedOrNull(useCase())?.recommended)
        coVerify(exactly = 0) { repository.getGamesByGenre(any(), any()) }
    }

    @Test
    fun `skips the personalised shelf when the library is too small to trust`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile(sampleSize = 2))

        assertNull(feedOrNull(useCase())?.recommended)
        coVerify(exactly = 0) { repository.getGamesByGenre(any(), any()) }
    }

    @Test
    fun `drops games the user already saved and those the generic shelves already show`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        every { repository.getSavedGames() } returns flowOf(listOf(rpgGame(10)))
        coEvery { repository.getPopularGames(any()) } returns AppResult.success(listOf(rpgGame(11)))
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.success(listOf(rpgGame(12)))
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        val shelf = feedOrNull(useCase())?.recommended

        assertEquals((13..20).toList(), shelf?.games?.map { it.id })
    }

    @Test
    fun `drops the personalised shelf when too little survives the pruning`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success(listOf(rpgGame(10), rpgGame(11)))

        assertNull(feedOrNull(useCase())?.recommended)
    }

    @Test
    fun `a failing personalised shelf leaves the generic feed intact`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.failure(RepositoryError.NoNetwork)

        val feed = feedOrNull(useCase())

        assertNull(feed?.recommended)
        assertEquals(listOf(PLAYING), feed?.popular)
        assertEquals(listOf(ANTICIPATED), feed?.upcoming)
    }
}
