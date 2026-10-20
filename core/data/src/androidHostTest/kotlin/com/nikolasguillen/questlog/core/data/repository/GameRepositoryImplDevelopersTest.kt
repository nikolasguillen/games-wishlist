package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.ReleaseNotificationDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import com.nikolasguillen.questlog.core.network.model.IgdbCompany
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbImage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the developer half of [GameRepositoryImpl]: the company search and profile queries, and the paged
 * lookups behind a developer's or a character's list of games ([GameRepositoryImpl.getDevelopedGames] and
 * [GameRepositoryImpl.getGamesByIds]).
 */
class GameRepositoryImplDevelopersTest {

    private val apiService = mockk<IgdbApiService>()

    private val repository = GameRepositoryImpl(
        apiService = apiService,
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        discoverCacheDao = mockk<DiscoverCacheDao>(relaxed = true),
        releaseNotificationDao = mockk<ReleaseNotificationDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

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

    private fun games(count: Int, firstId: Int = 1): List<IgdbGame> = List(count) { igdbGame(firstId + it) }

    @Test
    fun `a developer search matches the name by contains and only keeps companies that developed a game`() = runTest {
        val body = slot<String>()
        coEvery { apiService.getCompanies(capture(body)) } returns listOf(
            IgdbCompany(id = 9, name = "Capcom", logo = IgdbImage(1, "//x/t_thumb/c.jpg"), developed = listOf(1, 2, 3))
        )

        val result = repository.searchDevelopers("capcom")

        assertTrue(body.captured.contains("fields name, logo.url, developed;"))
        assertTrue(body.captured.contains("where name ~ *\"capcom\"* & developed != null;"))
        assertTrue(body.captured.contains("limit 500;"))
        val developer = (result as AppResult.Success).data.single()
        assertEquals("https://x/t_logo_med/c.png", developer.logoUrl)
        assertEquals(3, developer.developedGameCount)
    }

    @Test
    fun `a developer profile asks for the description, the logo and the founding date`() = runTest {
        val body = slot<String>()
        coEvery { apiService.getCompanies(capture(body)) } returns listOf(
            IgdbCompany(
                id = 9, name = "Naughty Dog", description = "", startDate = 473299200,
                logo = IgdbImage(2, "//x/t_thumb/n.jpg")
            )
        )

        val result = repository.getDeveloperProfile(9)

        assertTrue(body.captured.contains("fields name, description, logo.url, start_date;"))
        assertTrue(body.captured.contains("where id = 9;"))
        val profile = (result as AppResult.Success).data
        assertEquals(null, profile.description)
        assertEquals(1984, profile.foundedYear)
        assertEquals("https://x/t_logo_med/n.png", profile.logoUrl)
    }

    @Test
    fun `a developer profile with no row is an unknown failure`() = runTest {
        coEvery { apiService.getCompanies(any()) } returns emptyList()

        val result = repository.getDeveloperProfile(9)

        assertTrue((result as AppResult.Failure).error is RepositoryError.Unknown)
    }

    @Test
    fun `developed games are filtered to the developer role and sorted by id for stable paging`() = runTest {
        val body = slot<String>()
        coEvery { apiService.searchGames(capture(body)) } returns games(3)

        repository.getDevelopedGames(401)

        val query = body.captured
        assertTrue(query.contains("involved_companies.company = 401 & involved_companies.developer = true"))
        assertTrue(query.contains("version_parent = null"))
        assertTrue(query.contains("game_type != ("))
        assertTrue(query.contains("sort id asc;"))
        assertTrue(query.contains("limit 500;"))
        assertTrue(query.contains("offset 0;"))
    }

    @Test
    fun `a full page is followed by the next one and a short page ends the loop`() = runTest {
        val bodies = mutableListOf<String>()
        coEvery { apiService.searchGames(capture(bodies)) } returnsMany listOf(games(500), games(120, firstId = 501))

        val result = repository.getDevelopedGames(401)

        assertEquals(620, (result as AppResult.Success).data.size)
        assertEquals(2, bodies.size)
        assertTrue(bodies[1].contains("offset 500;"))
    }

    @Test
    fun `exactly one full page costs a third empty request to prove it was the last`() = runTest {
        coEvery { apiService.searchGames(any()) } returnsMany listOf(games(500), games(500, firstId = 501), emptyList())

        val result = repository.getDevelopedGames(401)

        assertEquals(1000, (result as AppResult.Success).data.size)
        coVerify(exactly = 3) { apiService.searchGames(any()) }
    }

    @Test
    fun `paging stops at ten pages even when every page is full`() = runTest {
        coEvery { apiService.searchGames(any()) } answers { games(500) }

        val result = repository.getDevelopedGames(401)

        assertEquals(5_000, (result as AppResult.Success).data.size)
        coVerify(exactly = 10) { apiService.searchGames(any()) }
    }

    @Test
    fun `a failure on the second page fails the whole lookup with the mapped error`() = runTest {
        coEvery { apiService.searchGames(any()) } returns games(500) andThenThrows
            IgdbHttpException(code = 503, message = "Service Unavailable")

        val result = repository.getDevelopedGames(401)

        assertEquals(RepositoryError.Http(503, "Service Unavailable"), (result as AppResult.Failure).error)
    }

    @Test
    fun `games by ids ask for the ids and drop the noisy types`() = runTest {
        val body = slot<String>()
        coEvery { apiService.searchGames(capture(body)) } returns games(2)

        val result = repository.getGamesByIds(listOf(1, 2))

        assertTrue(body.captured.contains("where id = (1,2) & game_type != ("))
        assertTrue(body.captured.contains("version_parent = null"))
        assertEquals(2, (result as AppResult.Success).data.size)
    }

    @Test
    fun `an empty id list makes no request`() = runTest {
        val result = repository.getGamesByIds(emptyList())

        assertEquals(emptyList<Any>(), (result as AppResult.Success).data)
        coVerify(exactly = 0) { apiService.searchGames(any()) }
    }

    @Test
    fun `a list of 750 ids is split into chunks of at most 500`() = runTest {
        val bodies = mutableListOf<String>()
        coEvery { apiService.searchGames(capture(bodies)) } answers { games(1) }

        repository.getGamesByIds((1..750).toList())

        assertEquals(2, bodies.size)
        assertEquals(500, bodies[0].substringAfter("id = (").substringBefore(")").split(",").size)
        assertEquals(250, bodies[1].substringAfter("id = (").substringBefore(")").split(",").size)
    }
}
