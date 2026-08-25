package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.local.WishlistCoverImageStorage
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.PlatformDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.network.IgdbApiService
import com.example.gameswishlist.core.network.model.IgdbGame
import com.example.gameswishlist.core.network.model.IgdbPopularityPrimitive
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the two Discover lanes ([GameRepositoryImpl.getPopularGames] and
 * [GameRepositoryImpl.getUpcomingGames]): the shared two-step fetch (rank on `/popularity_primitives`,
 * hydrate on `/games`) and the re-ranking that restores the popularity order the hydrate call loses.
 */
class GameRepositoryImplPopularGamesTest {

    private val apiService = mockk<IgdbApiService>()

    private val repository = GameRepositoryImpl(
        apiService = apiService,
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    private fun primitive(gameId: Int, value: Double) =
        IgdbPopularityPrimitive(gameId = gameId, value = value, popularityType = 2)

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

    @Test
    fun `getPopularGames restores the popularity ranking the hydrate call loses`() = runTest {
        // Popularity API ranks 3, 1, 2 by value...
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(
            primitive(gameId = 3, value = 90.0),
            primitive(gameId = 1, value = 50.0),
            primitive(gameId = 2, value = 10.0)
        )
        // ...but /games hydrates them in its own (id) order.
        coEvery { apiService.searchGames(any<RequestBody>()) } returns listOf(
            igdbGame(1), igdbGame(2), igdbGame(3)
        )

        val result = repository.getPopularGames()

        assertEquals(AppResult.success(listOf(3, 1, 2)), result.map { games -> games.map { it.id } })
    }

    @Test
    fun `getUpcomingGames also ranks by popularity through the shared two-step fetch`() = runTest {
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(
            primitive(gameId = 7, value = 80.0),
            primitive(gameId = 4, value = 30.0)
        )
        coEvery { apiService.searchGames(any<RequestBody>()) } returns listOf(igdbGame(4), igdbGame(7))

        val result = repository.getUpcomingGames()

        assertEquals(AppResult.success(listOf(7, 4)), result.map { games -> games.map { it.id } })
    }

    @Test
    fun `getPopularGames returns empty without hydrating when nothing is trending`() = runTest {
        coEvery { apiService.getPopularityPrimitives(any()) } returns emptyList()

        val result = repository.getPopularGames()

        assertEquals(AppResult.success(emptyList<Int>()), result.map { games -> games.map { it.id } })
        coVerify(exactly = 0) { apiService.searchGames(any<RequestBody>()) }
    }
}
