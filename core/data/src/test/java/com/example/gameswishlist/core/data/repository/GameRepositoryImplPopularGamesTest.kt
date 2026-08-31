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
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the two Discover lanes ([GameRepositoryImpl.getPopularGames] and
 * [GameRepositoryImpl.getUpcomingGames]): the shared two-step fetch (rank on `/popularity_primitives`,
 * hydrate on `/games`), the re-ranking that restores the popularity order the hydrate call loses, and
 * the platform filter — which is applied to the hydrate call only, and dropped entirely when the user
 * has picked nothing.
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

    private fun RequestBody.asText(): String = Buffer().also { writeTo(it) }.readUtf8()

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

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(listOf(3, 1, 2)), result.map { games -> games.map { it.id } })
    }

    @Test
    fun `getUpcomingGames also ranks by popularity through the shared two-step fetch`() = runTest {
        coEvery { apiService.getPopularityPrimitives(any()) } returns listOf(
            primitive(gameId = 7, value = 80.0),
            primitive(gameId = 4, value = 30.0)
        )
        coEvery { apiService.searchGames(any<RequestBody>()) } returns listOf(igdbGame(4), igdbGame(7))

        val result = repository.getUpcomingGames(emptySet())

        assertEquals(AppResult.success(listOf(7, 4)), result.map { games -> games.map { it.id } })
    }

    @Test
    fun `getPopularGames returns empty without hydrating when nothing is trending`() = runTest {
        coEvery { apiService.getPopularityPrimitives(any()) } returns emptyList()

        val result = repository.getPopularGames(emptySet())

        assertEquals(AppResult.success(emptyList<Int>()), result.map { games -> games.map { it.id } })
        coVerify(exactly = 0) { apiService.searchGames(any<RequestBody>()) }
    }

    @Test
    fun `the selected platforms narrow the hydrate call and not the ranking call`() = runTest {
        val rankingBody = slot<RequestBody>()
        val hydrateBody = slot<RequestBody>()
        coEvery { apiService.getPopularityPrimitives(capture(rankingBody)) } returns
            listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(capture(hydrateBody)) } returns listOf(igdbGame(1))

        repository.getPopularGames(setOf(48, 130))

        assertTrue(hydrateBody.captured.asText().contains("platforms = (48,130)"))
        // The Popularity API has no platform field, so pushing the filter down there would 400.
        assertFalse(rankingBody.captured.asText().contains("platforms"))
    }

    @Test
    fun `getGamesByGenre filters on the genre and floors the rating count`() = runTest {
        val body = slot<RequestBody>()
        coEvery { apiService.searchGames(capture(body)) } returns listOf(igdbGame(1))

        repository.getGamesByGenre(genreId = 12, platformIds = setOf(48))

        val query = body.captured.asText()
        assertTrue(query.contains("genres = (12)"))
        assertTrue(query.contains("platforms = (48)"))
        // Without the floor, `sort total_rating desc` hands the shelf to single-review curiosities.
        assertTrue(query.contains("total_rating_count >="))
        assertTrue(query.contains("sort total_rating desc"))
    }

    @Test
    fun `getGamesByGenre needs no ranking call of its own`() = runTest {
        coEvery { apiService.searchGames(any<RequestBody>()) } returns listOf(igdbGame(1))

        repository.getGamesByGenre(genreId = 12, platformIds = emptySet())

        coVerify(exactly = 0) { apiService.getPopularityPrimitives(any()) }
    }

    @Test
    fun `an empty selection drops the platform clause instead of filtering on nothing`() = runTest {
        val hydrateBody = slot<RequestBody>()
        coEvery { apiService.getPopularityPrimitives(any()) } returns
            listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(capture(hydrateBody)) } returns listOf(igdbGame(1))

        repository.getUpcomingGames(emptySet())

        assertFalse(hydrateBody.captured.asText().contains("platforms ="))
    }
}
