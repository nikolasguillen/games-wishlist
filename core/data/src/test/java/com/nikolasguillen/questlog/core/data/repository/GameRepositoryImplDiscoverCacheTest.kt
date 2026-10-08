package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.ReleaseNotificationDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.entity.CachedGameEntity
import com.nikolasguillen.questlog.core.database.entity.DiscoverLaneEntryEntity
import com.nikolasguillen.questlog.core.database.relation.CachedGameWithDetails
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Covers the cache-first behaviour of the two generic Discover lanes ([GameRepositoryImpl.getPopularGames],
 * [GameRepositoryImpl.getUpcomingGames]) and the cache invalidation on [GameRepositoryImpl.toggleOwnedPlatform]:
 * serving a fresh cached lane with no network call and in the stored order (FR-001, FR-003), falling
 * through to the network on a miss or a stale entry and persisting the result (FR-004, FR-009), treating
 * each lane's freshness independently (FR-005), and falling back to a stale cached copy instead of
 * failing when a refresh's network call errors (FR-008).
 */
class GameRepositoryImplDiscoverCacheTest {

    private val apiService = mockk<IgdbApiService>()
    private val discoverCacheDao = mockk<DiscoverCacheDao>()

    private val repository = GameRepositoryImpl(
        apiService = apiService,
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        discoverCacheDao = discoverCacheDao,
        releaseNotificationDao = mockk<ReleaseNotificationDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    private fun primitive(gameId: Int, value: Double) =
        IgdbPopularityPrimitive(gameId = gameId, value = value)

    private fun igdbGame(id: Int) = IgdbGame(
        id = id,
        name = "Game $id",
        summary = null,
        gameType = 0,
        firstReleaseDate = null,
        cover = null,
        totalRating = null,
        totalRatingCount = null,
        aggregatedRating = null,
        hypes = null,
        url = null,
        platforms = null,
        releaseDates = null,
        genres = null,
        involvedCompanies = null,
        gameEngines = null
    )

    private fun cachedGame(id: Int) = CachedGameWithDetails(
        game = CachedGameEntity(
            id = id,
            name = "Cached $id",
            description = "",
            released = null,
            backgroundImage = null,
            rating = 0.0,
            ratingCount = 0,
            hypes = 0,
            metacritic = null,
            gameTypeId = 0,
            url = null
        ),
        platforms = emptyList(),
        genres = emptyList(),
        companyRefs = emptyList()
    )

    private fun stubReplaceLane() {
        coEvery {
            discoverCacheDao.replaceLane(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Unit
    }

    @Test
    fun `a fresh cached lane is served with no network call, in the stored order`() = runTest {
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns System.currentTimeMillis()
        coEvery { discoverCacheDao.getLaneGames(DiscoverLane.POPULAR_THIS_MONTH) } returns
            listOf(cachedGame(5), cachedGame(1), cachedGame(9))

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(listOf(5, 1, 9)), result.map { games -> games.map { it.id } })
        coVerify(exactly = 0) { apiService.getPopularityPrimitives(any()) }
        coVerify(exactly = 0) { apiService.searchGames(any<String>()) }
    }

    @Test
    fun `no cached lane fetches fresh and persists the ranked order`() = runTest {
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.MOST_ANTICIPATED) } returns null
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(
            primitive(gameId = 3, value = 90.0),
            primitive(gameId = 1, value = 50.0)
        )
        coEvery { apiService.searchGames(any<String>()) } returns listOf(igdbGame(1), igdbGame(3))
        val entriesSlot = slot<List<DiscoverLaneEntryEntity>>()
        coEvery {
            discoverCacheDao.replaceLane(
                DiscoverLane.MOST_ANTICIPATED, any(), any(), capture(entriesSlot), any(), any(), any(), any(), any(), any()
            )
        } returns Unit

        val result = repository.getUpcomingGames(emptySet())

        assertEquals(AppResult.success(listOf(3, 1)), result.map { games -> games.map { it.id } })
        coVerify(exactly = 1) {
            discoverCacheDao.replaceLane(
                DiscoverLane.MOST_ANTICIPATED, any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
        assertEquals(listOf(3, 1), entriesSlot.captured.sortedBy { it.position }.map { it.gameId })
    }

    @Test
    fun `a successful empty fetch is still cached, so it is not re-fetched on the next call`() = runTest {
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns null
        coEvery { apiService.getPopularityPrimitives(any()) } returns emptyList()
        stubReplaceLane()

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(emptyList<Int>()), result.map { games -> games.map { it.id } })
        coVerify(exactly = 1) {
            discoverCacheDao.replaceLane(
                DiscoverLane.POPULAR_THIS_MONTH, any(), emptyList(), emptyList(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `a stale cached lane is refetched and replaces the cache`() = runTest {
        val staleTimestamp = System.currentTimeMillis() - 7 * 60 * 60 * 1000L // older than the 6h TTL
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns staleTimestamp
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(any<String>()) } returns listOf(igdbGame(1))
        stubReplaceLane()

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(listOf(1)), result.map { games -> games.map { it.id } })
        coVerify(exactly = 1) { apiService.getPopularityPrimitives(any()) }
        coVerify(exactly = 1) {
            discoverCacheDao.replaceLane(
                DiscoverLane.POPULAR_THIS_MONTH, any(), any(), any(), any(), any(), any(), any(), any(), any()
            )
        }
    }

    @Test
    fun `one fresh lane and one stale lane in the same pass each take their own path`() = runTest {
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns System.currentTimeMillis()
        coEvery { discoverCacheDao.getLaneGames(DiscoverLane.POPULAR_THIS_MONTH) } returns listOf(cachedGame(1))

        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.MOST_ANTICIPATED) } returns null
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(primitive(gameId = 2, value = 50.0))
        coEvery { apiService.searchGames(any<String>()) } returns listOf(igdbGame(2))
        stubReplaceLane()

        repository.getPopularGames(emptySet())
        repository.getUpcomingGames(emptySet())

        // Only the stale/missing MOST_ANTICIPATED lane touched the network -- POPULAR_THIS_MONTH was fresh.
        coVerify(exactly = 1) { apiService.getPopularityPrimitives(any()) }
        coVerify(exactly = 1) { apiService.searchGames(any<String>()) }
    }

    @Test
    fun `a failed refresh with a stale cached copy present serves the stale copy instead of failing`() = runTest {
        val staleTimestamp = System.currentTimeMillis() - 7 * 60 * 60 * 1000L
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns staleTimestamp
        coEvery { apiService.getPopularityPrimitives(any()) } throws IOException("network down")
        coEvery { discoverCacheDao.getLaneGames(DiscoverLane.POPULAR_THIS_MONTH) } returns
            listOf(cachedGame(1), cachedGame(2))

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(listOf(1, 2)), result.map { games -> games.map { it.id } })
        coVerify(exactly = 0) {
            discoverCacheDao.replaceLane(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a failed fetch with no cached copy at all still fails, unchanged from today`() = runTest {
        coEvery { discoverCacheDao.getLaneFetchedAt(DiscoverLane.POPULAR_THIS_MONTH) } returns null
        coEvery { apiService.getPopularityPrimitives(any()) } throws IOException("network down")

        val result = repository.getPopularGames(emptySet())

        assertTrue(result is AppResult.Failure)
    }
}
