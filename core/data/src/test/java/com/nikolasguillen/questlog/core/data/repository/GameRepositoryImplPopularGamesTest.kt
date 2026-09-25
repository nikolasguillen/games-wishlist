package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
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
    fun `getPopularGames's release filter carries no TBD escape hatch`() = runTest {
        val hydrateBody = slot<RequestBody>()
        coEvery { apiService.getPopularityPrimitives(any()) } returns
            listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(capture(hydrateBody)) } returns listOf(igdbGame(1))

        repository.getPopularGames(emptySet())

        val query = hydrateBody.captured.asText()
        assertTrue(query.contains("first_release_date != null & first_release_date <="))
        // The undated-but-pending admission rule is upcoming-lane only -- FR-007 forbids it here.
        assertFalse(query.contains("release_dates.date_format"))
    }

    @Test
    fun `getUpcomingGames admits an undated game with a pending release_dates entry`() = runTest {
        val hydrateBody = slot<RequestBody>()
        coEvery { apiService.getPopularityPrimitives(any()) } returns
            listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(capture(hydrateBody)) } returns listOf(igdbGame(1))

        repository.getUpcomingGames(setOf(48))

        val query = hydrateBody.captured.asText()
        // The future-date branch is untouched...
        assertTrue(query.contains("(first_release_date > "))
        // ...and the new TBD branch is OR'd into it...
        assertTrue(query.contains("first_release_date = null & release_dates.date_format = 7"))
        // ...both wrapped so the closing parens land before the platform clause reattaches, not
        // splicing into the middle of the release expression.
        assertTrue(query.contains(")) & platforms = (48)"))
    }

    @Test
    fun `the TBD branch only applies to games with no first_release_date at all`() = runTest {
        val hydrateBody = slot<RequestBody>()
        coEvery { apiService.getPopularityPrimitives(any()) } returns
            listOf(primitive(gameId = 1, value = 50.0))
        coEvery { apiService.searchGames(capture(hydrateBody)) } returns listOf(igdbGame(1))

        repository.getUpcomingGames(emptySet())

        // The date_format check is AND'd onto first_release_date = null, never standalone -- a game
        // with a past first_release_date satisfies neither this branch nor the future-date one, so a
        // stale TBD marker on an already-released game can't resurrect it into the shelf.
        assertTrue(
            hydrateBody.captured.asText().contains("first_release_date = null & release_dates.date_format")
        )
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
    fun `getGamesByGenre bounds the pool to a recent release window`() = runTest {
        val body = slot<RequestBody>()
        coEvery { apiService.searchGames(capture(body)) } returns listOf(igdbGame(1))

        repository.getGamesByGenre(genreId = 12, platformIds = emptySet())

        val windowStart = body.captured.asText()
            .substringAfter("first_release_date > ")
            .takeWhile { it.isDigit() }
            .toLong()
        // A lower bound only: an unreleased game in the genre is still a candidate. The window itself is
        // asserted loosely -- pinning the exact constant would only restate it.
        val nowSeconds = System.currentTimeMillis() / 1000
        assertTrue(windowStart < nowSeconds)
        assertTrue(windowStart > nowSeconds - 30L * 365 * 24 * 60 * 60)
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
