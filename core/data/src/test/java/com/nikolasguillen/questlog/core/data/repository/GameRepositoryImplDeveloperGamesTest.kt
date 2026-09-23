package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.getGamesByDeveloper], the "More from <studio>" personalised Discover
 * shelf's coarse fetch: filters on the developer rather than a genre, carries no rating-count floor
 * (unlike [GameRepositoryImpl.getGamesByGenre] — this shelf exists to surface a followed studio's
 * unreleased titles, which a floor would hide outright), and the platform filter behaves exactly as it
 * does for the other Discover lanes.
 */
class GameRepositoryImplDeveloperGamesTest {

    private val apiService = mockk<IgdbApiService>()

    private val repository = GameRepositoryImpl(
        apiService = apiService,
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
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

    private fun RequestBody.asText(): String = Buffer().also { writeTo(it) }.readUtf8()

    @Test
    fun `filters on the developer and carries no rating-count floor`() = runTest {
        val body = slot<RequestBody>()
        coEvery { apiService.searchGames(capture(body)) } returns listOf(igdbGame(1))

        repository.getGamesByDeveloper(companyId = 50, platformIds = setOf(48))

        val query = body.captured.asText()
        assertTrue(query.contains("involved_companies.company = (50)"))
        assertTrue(query.contains("platforms = (48)"))
        // Unlike getGamesByGenre, no floor: an unreleased title from a followed studio has no ratings
        // yet, and that is exactly what this shelf exists to surface.
        assertFalse(query.contains("total_rating_count >="))
        assertTrue(query.contains("sort first_release_date desc"))
    }

    @Test
    fun `an empty selection drops the platform clause instead of filtering on nothing`() = runTest {
        val body = slot<RequestBody>()
        coEvery { apiService.searchGames(capture(body)) } returns listOf(igdbGame(1))

        repository.getGamesByDeveloper(companyId = 50, platformIds = emptySet())

        assertFalse(body.captured.asText().contains("platforms ="))
    }
}
