package com.nikolasguillen.questlog.core.domain.usecase.discover

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Company
import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.ShelfReason
import com.nikolasguillen.questlog.core.model.TasteProfile
import com.nikolasguillen.questlog.core.model.TasteSignal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val PLAYING = Game(id = 1, name = "Cindergate")
private val ANTICIPATED = Game(id = 2, name = "Ashborne Reverie")

private val RPG = Genre(id = 12, name = "RPG")
private val SHOOTER = Genre(id = 5, name = "Shooter")
private val PLATFORMER = Genre(id = 7, name = "Platformer")

private val LARIAN = Company(id = 50, name = "Larian Studios")
private val FROM_SOFTWARE = Company(id = 51, name = "FromSoftware")

/** Enough saved games to clear the use case's minimum sample size. */
private const val TRUSTED_SAMPLE_SIZE = 8

/** Enough saved games from one developer to clear the use case's recurrence threshold. */
private const val TRUSTED_DEVELOPER_COUNT = 2

private const val PAST_DATE = "2000-01-01"
private const val FUTURE_DATE = "2099-01-01"

private fun rpgGame(id: Int, rating: Double = 0.0, ratingCount: Int = 0) =
    Game(id = id, name = "RPG $id", rating = rating, ratingCount = ratingCount, genres = listOf(RPG))

private fun platformerGame(id: Int, rating: Double = 0.0, ratingCount: Int = 0) = Game(
    id = id, name = "Platformer $id", rating = rating, ratingCount = ratingCount, genres = listOf(PLATFORMER)
)

private fun developerGame(
    id: Int,
    developer: Company = LARIAN,
    genres: List<Genre> = listOf(RPG),
    rating: Double = 0.0,
    ratingCount: Int = 0,
    hypes: Int = 0,
    releaseDate: String? = null
) = Game(
    id = id,
    name = "Game $id",
    genres = genres,
    developers = listOf(developer),
    rating = rating,
    ratingCount = ratingCount,
    hypes = hypes,
    releaseDate = releaseDate
)

/**
 * Covers [GetDiscoverFeedUseCase]: the two generic shelves are combined into one [DiscoverFeed] when
 * both sources succeed, a single generic failure fails the whole feed, the platforms the user picked in
 * Settings reach every shelf, and the personalised shelves appear only for signals the taste profile
 * earns — a recurring developer first when the library earns one, then the strongest genres, capped at
 * two total — pruned of saved games, of duplicates from the generic shelves, and of duplicates across
 * each other. The developer shelf additionally covers its own ordering (unreleased titles by hype, then
 * released titles by confidence) and its own local filter (publisher-only results dropped). Also covers
 * the two ways the feed can change after the initial load without the platform selection moving: an
 * explicit [refresh][GetDiscoverFeedUseCase.invoke] re-fetches everything, while a taste-profile edit
 * that moves the recommendation plan only flags [DiscoverFeed.hasStaleRecommendations] — the whole point
 * being that the second case costs no network call.
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

    /** A profile the use case will trust, leaning towards [RPG] and away from [SHOOTER]. No developer signal. */
    private fun rpgProfile(sampleSize: Int = TRUSTED_SAMPLE_SIZE) = TasteProfile(
        genres = mapOf(RPG.id to TasteSignal(1.0, 1), SHOOTER.id to TasteSignal(-0.4, 0)),
        sampleSize = sampleSize
    )

    /** A profile with two positive genres, [RPG] comfortably ahead of [PLATFORMER]. No developer signal. */
    private fun multiGenreProfile(sampleSize: Int = TRUSTED_SAMPLE_SIZE) = TasteProfile(
        genres = mapOf(RPG.id to TasteSignal(1.0, 1), PLATFORMER.id to TasteSignal(0.5, 1), SHOOTER.id to TasteSignal(-0.4, 0)),
        sampleSize = sampleSize
    )

    /**
     * A profile where [LARIAN] has earned the developer shelf and there is no positive genre, so the
     * plan is the developer shelf alone -- the second slot stays unused rather than pulling in a genre
     * query that would only be incidental to what the test is covering.
     */
    private fun developerProfile(
        developerCount: Int = TRUSTED_DEVELOPER_COUNT,
        sampleSize: Int = TRUSTED_SAMPLE_SIZE
    ) = TasteProfile(
        developers = mapOf(LARIAN.id to TasteSignal(0.8, developerCount)),
        sampleSize = sampleSize
    )

    /** A profile where [LARIAN] has earned the developer shelf and [RPG] fills the remaining slot. */
    private fun developerAndGenreProfile(sampleSize: Int = TRUSTED_SAMPLE_SIZE) = TasteProfile(
        genres = mapOf(RPG.id to TasteSignal(1.0, 1)),
        developers = mapOf(LARIAN.id to TasteSignal(0.8, TRUSTED_DEVELOPER_COUNT)),
        sampleSize = sampleSize
    )

    private fun feedOrNull(result: AppResult<DiscoverFeed>) = (result as? AppResult.Success)?.data

    @Test
    fun `combines both shelves when both sources succeed`() = runTest {
        val result = useCase().first()

        assertEquals(
            AppResult.success(DiscoverFeed(popular = listOf(PLAYING), upcoming = listOf(ANTICIPATED))),
            result
        )
    }

    @Test
    fun `fails the feed when the popular source fails`() = runTest {
        coEvery { repository.getPopularGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase().first())
    }

    @Test
    fun `fails the feed when the upcoming source fails`() = runTest {
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase().first())
    }

    @Test
    fun `passes the selected platforms to every shelf`() = runTest {
        val selection = setOf(48, 130)
        every { getSelectedPlatformIds() } returns flowOf(selection)
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(any(), any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        useCase().first()

        coVerify { repository.getPopularGames(selection) }
        coVerify { repository.getUpcomingGames(selection) }
        coVerify { repository.getGamesByGenre(RPG.id, selection) }
    }

    @Test
    fun `builds a personalised shelf from the strongest positive genre`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        val shelf = feedOrNull(useCase().first())?.recommended?.firstOrNull()

        assertEquals(RPG, (shelf?.reason as? ShelfReason.ByGenre)?.genre)
        assertEquals((10..20).toList(), shelf?.games?.map { it.id })
    }

    @Test
    fun `builds a shelf for each of the two strongest positive genres, strongest first, with no qualifying developer`() = runTest {
        every { getTasteProfile() } returns flowOf(multiGenreProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        coEvery { repository.getGamesByGenre(PLATFORMER.id, any()) } returns
            AppResult.success((30..40).map { platformerGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertEquals(listOf(RPG, PLATFORMER), shelves.map { (it.reason as ShelfReason.ByGenre).genre })
    }

    @Test
    fun `never queries beyond the two strongest positive genres`() = runTest {
        val thirdGenre = Genre(id = 21, name = "Strategy")
        every { getTasteProfile() } returns flowOf(
            TasteProfile(
                genres = mapOf(
                    RPG.id to TasteSignal(1.0, 1),
                    PLATFORMER.id to TasteSignal(0.5, 1),
                    thirdGenre.id to TasteSignal(0.2, 1)
                ),
                sampleSize = TRUSTED_SAMPLE_SIZE
            )
        )
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        coEvery { repository.getGamesByGenre(PLATFORMER.id, any()) } returns
            AppResult.success((30..40).map { platformerGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertEquals(2, shelves.size)
        coVerify(exactly = 0) { repository.getGamesByGenre(thirdGenre.id, any()) }
    }

    @Test
    fun `keeps a game that qualifies for two genres only in the stronger genre's shelf`() = runTest {
        every { getTasteProfile() } returns flowOf(multiGenreProfile())
        val hybrid = Game(id = 99, name = "Hybrid", genres = listOf(RPG, PLATFORMER))
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success(listOf(hybrid) + (10..17).map { rpgGame(it) })
        coEvery { repository.getGamesByGenre(PLATFORMER.id, any()) } returns
            AppResult.success(listOf(hybrid) + (30..37).map { platformerGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertTrue(99 in shelves[0].games.map { it.id })
        assertFalse(99 in shelves[1].games.map { it.id })
    }

    @Test
    fun `a failing second shelf does not take the first shelf down with it`() = runTest {
        every { getTasteProfile() } returns flowOf(multiGenreProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        coEvery { repository.getGamesByGenre(PLATFORMER.id, any()) } returns
            AppResult.failure(RepositoryError.NoNetwork)

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertEquals(listOf(RPG), shelves.map { (it.reason as ShelfReason.ByGenre).genre })
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

        val shelf = feedOrNull(useCase().first())?.recommended?.firstOrNull()

        // The two-vote 100 is pushed below the scores people actually voted on, but it is still on the
        // shelf and still ahead of a mediocre game -- a hard rating-count floor would have dropped it,
        // along with every niche and newly released game in the genre.
        assertEquals(listOf(32, 31, 30, 33), shelf?.games?.map { it.id })
    }

    @Test
    fun `leads with the developer shelf when the library earns one, capping genres to the remaining slot`() = runTest {
        every { getTasteProfile() } returns flowOf(
            TasteProfile(
                genres = mapOf(RPG.id to TasteSignal(1.0, 1), PLATFORMER.id to TasteSignal(0.5, 1)),
                developers = mapOf(LARIAN.id to TasteSignal(0.8, TRUSTED_DEVELOPER_COUNT)),
                sampleSize = TRUSTED_SAMPLE_SIZE
            )
        )
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.success((50..60).map { developerGame(it) })
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertEquals(2, shelves.size)
        assertEquals(LARIAN, (shelves[0].reason as ShelfReason.ByDeveloper).developer)
        assertEquals(RPG, (shelves[1].reason as ShelfReason.ByGenre).genre)
        coVerify(exactly = 0) { repository.getGamesByGenre(PLATFORMER.id, any()) }
    }

    @Test
    fun `a developer with only one saved game does not earn a shelf, falling back to two genres`() = runTest {
        every { getTasteProfile() } returns flowOf(
            TasteProfile(
                genres = mapOf(RPG.id to TasteSignal(1.0, 1), PLATFORMER.id to TasteSignal(0.5, 1)),
                developers = mapOf(LARIAN.id to TasteSignal(0.8, 1)),
                sampleSize = TRUSTED_SAMPLE_SIZE
            )
        )
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        coEvery { repository.getGamesByGenre(PLATFORMER.id, any()) } returns
            AppResult.success((30..40).map { platformerGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertEquals(listOf(RPG, PLATFORMER), shelves.map { (it.reason as ShelfReason.ByGenre).genre })
        coVerify(exactly = 0) { repository.getGamesByDeveloper(any(), any()) }
    }

    @Test
    fun `keeps a game that qualifies for both the developer and a genre shelf only in the developer shelf`() = runTest {
        every { getTasteProfile() } returns flowOf(developerAndGenreProfile())
        val hybrid = developerGame(id = 99, genres = listOf(RPG))
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.success(listOf(hybrid) + (50..57).map { developerGame(it) })
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success(listOf(hybrid) + (10..17).map { rpgGame(it) })

        val shelves = feedOrNull(useCase().first())?.recommended.orEmpty()

        assertTrue(99 in shelves[0].games.map { it.id })
        assertFalse(99 in shelves[1].games.map { it.id })
    }

    @Test
    fun `orders the developer shelf with unreleased titles first by hype, then released titles by confidence`() = runTest {
        every { getTasteProfile() } returns flowOf(developerProfile())
        val releasedHigh = developerGame(id = 50, rating = 90.0, ratingCount = 100, releaseDate = PAST_DATE)
        val releasedLow = developerGame(id = 51, rating = 60.0, ratingCount = 100, releaseDate = PAST_DATE)
        val upcomingLowHype = developerGame(id = 52, hypes = 5, releaseDate = FUTURE_DATE)
        val upcomingHighHype = developerGame(id = 53, hypes = 50, releaseDate = FUTURE_DATE)
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.success(listOf(releasedHigh, releasedLow, upcomingLowHype, upcomingHighHype))

        val shelf = feedOrNull(useCase().first())?.recommended?.firstOrNull()

        assertEquals(listOf(53, 52, 50, 51), shelf?.games?.map { it.id })
    }

    @Test
    fun `filters out games where the developer only published, not developed`() = runTest {
        every { getTasteProfile() } returns flowOf(developerProfile())
        val trueDeveloperGames = (50..57).map { developerGame(it) }
        val publisherOnly = Game(id = 99, name = "Published Only", publishers = listOf(LARIAN), genres = listOf(RPG))
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.success(trueDeveloperGames + publisherOnly)

        val shelf = feedOrNull(useCase().first())?.recommended?.firstOrNull()

        assertFalse(99 in shelf?.games.orEmpty().map { it.id })
    }

    @Test
    fun `a failing developer shelf leaves the genre shelf and the generic feed intact`() = runTest {
        every { getTasteProfile() } returns flowOf(developerAndGenreProfile())
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.failure(RepositoryError.NoNetwork)
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })

        val feed = feedOrNull(useCase().first())

        assertEquals(listOf(RPG), feed?.recommended?.map { (it.reason as ShelfReason.ByGenre).genre })
        assertEquals(listOf(PLAYING), feed?.popular)
        assertEquals(listOf(ANTICIPATED), feed?.upcoming)
    }

    @Test
    fun `re-emits the whole feed when the user changes their platform selection`() = runTest {
        val selection = MutableStateFlow(setOf(48))
        every { getSelectedPlatformIds() } returns selection
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        selection.value = setOf(130)
        advanceUntilIdle()
        collection.cancel()

        assertEquals(2, feeds.size)
        coVerify { repository.getPopularGames(setOf(48)) }
        coVerify { repository.getPopularGames(setOf(130)) }
    }

    @Test
    fun `does not refetch when the selection is re-emitted unchanged`() = runTest {
        // Room re-emits on any write to the table, including ones that left the selection alone.
        val selection = MutableStateFlow(setOf(48))
        every { getSelectedPlatformIds() } returns selection
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        selection.value = setOf(48)
        advanceUntilIdle()
        collection.cancel()

        assertEquals(1, feeds.size)
        coVerify(exactly = 1) { repository.getPopularGames(any()) }
    }

    @Test
    fun `never recommends a genre the user has dropped`() = runTest {
        // Only SHOOTER has a weight, and it is negative: a rejection is not a recommendation.
        every { getTasteProfile() } returns
            flowOf(TasteProfile(genres = mapOf(SHOOTER.id to TasteSignal(-1.0, 0)), sampleSize = TRUSTED_SAMPLE_SIZE))

        assertTrue(feedOrNull(useCase().first())?.recommended.isNullOrEmpty())
        coVerify(exactly = 0) { repository.getGamesByGenre(any(), any()) }
    }

    @Test
    fun `skips the personalised shelves when the library is too small to trust`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile(sampleSize = 2))

        assertTrue(feedOrNull(useCase().first())?.recommended.isNullOrEmpty())
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

        val shelf = feedOrNull(useCase().first())?.recommended?.firstOrNull()

        assertEquals((13..20).toList(), shelf?.games?.map { it.id })
    }

    @Test
    fun `drops a personalised shelf when too little survives the pruning`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success(listOf(rpgGame(10), rpgGame(11)))

        assertTrue(feedOrNull(useCase().first())?.recommended.isNullOrEmpty())
    }

    @Test
    fun `a failing personalised shelf leaves the generic feed intact`() = runTest {
        every { getTasteProfile() } returns flowOf(rpgProfile())
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.failure(RepositoryError.NoNetwork)

        val feed = feedOrNull(useCase().first())

        assertTrue(feed?.recommended.isNullOrEmpty())
        assertEquals(listOf(PLAYING), feed?.popular)
        assertEquals(listOf(ANTICIPATED), feed?.upcoming)
    }

    @Test
    fun `a fresh feed is never flagged stale`() = runTest {
        val result = useCase().first()

        assertFalse(feedOrNull(result)?.hasStaleRecommendations ?: true)
    }

    @Test
    fun `reloads the whole feed on an explicit refresh`() = runTest {
        val refresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase(refresh).toList(feeds) }
        advanceUntilIdle()

        refresh.tryEmit(Unit)
        advanceUntilIdle()
        collection.cancel()

        assertEquals(2, feeds.size)
        coVerify(exactly = 2) { repository.getPopularGames(any()) }
    }

    @Test
    fun `flags the feed stale when the strongest genre moves without a refresh`() = runTest {
        val profile = MutableStateFlow(rpgProfile())
        every { getTasteProfile() } returns profile
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        // SHOOTER overtakes RPG without any platform change or refresh -- e.g. the user saved a batch
        // of shooters and returned to the feed.
        profile.value = TasteProfile(genres = mapOf(SHOOTER.id to TasteSignal(1.0, 1)), sampleSize = TRUSTED_SAMPLE_SIZE)
        advanceUntilIdle()
        collection.cancel()

        assertFalse(feedOrNull(feeds.first())?.hasStaleRecommendations ?: true)
        assertTrue(feedOrNull(feeds.last())?.hasStaleRecommendations ?: false)
        // The flag is derived locally -- nothing was re-fetched to raise it.
        coVerify(exactly = 1) { repository.getPopularGames(any()) }
        coVerify(exactly = 1) { repository.getGamesByGenre(any(), any()) }
    }

    @Test
    fun `does not flag the feed stale when a library edit leaves the strongest genre unchanged`() = runTest {
        val profile = MutableStateFlow(rpgProfile())
        every { getTasteProfile() } returns profile
        coEvery { repository.getGamesByGenre(RPG.id, any()) } returns
            AppResult.success((10..20).map { rpgGame(it) })
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        // The library grows but RPG is still comfortably the strongest genre.
        profile.value = rpgProfile(sampleSize = TRUSTED_SAMPLE_SIZE + 5)
        advanceUntilIdle()
        collection.cancel()

        // No second emission at all: the recommendation plan, not the raw profile, is what the feed
        // reacts to, and it did not change.
        assertEquals(1, feeds.size)
    }

    @Test
    fun `flags the feed stale once the library earns a personalised shelf it did not have yet`() = runTest {
        val profile = MutableStateFlow(TasteProfile.EMPTY)
        every { getTasteProfile() } returns profile
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        profile.value = rpgProfile()
        advanceUntilIdle()
        collection.cancel()

        assertTrue(feedOrNull(feeds.last())?.hasStaleRecommendations ?: false)
    }

    @Test
    fun `flags the feed stale when the recommended developer changes without a refresh`() = runTest {
        val profile = MutableStateFlow(developerProfile())
        every { getTasteProfile() } returns profile
        coEvery { repository.getGamesByDeveloper(LARIAN.id, any()) } returns
            AppResult.success((50..60).map { developerGame(it) })
        val feeds = mutableListOf<AppResult<DiscoverFeed>>()
        val collection = launch(UnconfinedTestDispatcher(testScheduler)) { useCase().toList(feeds) }
        advanceUntilIdle()

        // FROM_SOFTWARE overtakes LARIAN without any platform change or refresh.
        profile.value = TasteProfile(
            developers = mapOf(FROM_SOFTWARE.id to TasteSignal(1.0, TRUSTED_DEVELOPER_COUNT)),
            sampleSize = TRUSTED_SAMPLE_SIZE
        )
        advanceUntilIdle()
        collection.cancel()

        assertFalse(feedOrNull(feeds.first())?.hasStaleRecommendations ?: true)
        assertTrue(feedOrNull(feeds.last())?.hasStaleRecommendations ?: false)
        coVerify(exactly = 1) { repository.getPopularGames(any()) }
    }
}
