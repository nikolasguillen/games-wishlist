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
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.getPopularGamesByGenre], Search's genre browse: the one `/games` query it sends
 * (the genre, a released-only upper bound, a two-year lower bound, the rating floor and the sort by how many
 * people rated a game), that it applies no platform filter, that it hands the rows back in the order IGDB
 * ranked them, and how a failure and a cancellation leave it.
 */
class GameRepositoryImplGenreBrowseTest {

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

    /** Sends a browse and returns the query body the repository put on the wire. */
    private suspend fun capturedBody(genreId: Int = 12): String {
        val body = slot<String>()
        coEvery { apiService.searchGames(capture(body)) } returns emptyList()
        repository.getPopularGamesByGenre(genreId)
        return body.captured
    }

    private fun String.seconds(after: String): Long {
        val match = Regex(Regex.escape(after) + """\s*(\d+)""").find(this)
        assertNotNull("'$after' <seconds> not in the query: $this", match)
        return match!!.groupValues[1].toLong()
    }

    @Test
    fun `the query asks for the genre, covers, no variants and the rating floor`() = runTest {
        val query = capturedBody(genreId = 12)

        assertTrue(query.contains("genres = (12)"))
        assertTrue(query.contains("cover != null"))
        assertTrue(query.contains("version_parent = null"))
        assertTrue(query.contains("total_rating_count >= 10"))
    }

    @Test
    fun `the query bounds the release date to released games from the last two years`() = runTest {
        val before = Clock.System.now().toEpochMilliseconds() / 1000
        val query = capturedBody()
        val after = Clock.System.now().toEpochMilliseconds() / 1000

        val upperBound = query.seconds("first_release_date <=")
        val lowerBound = query.seconds("first_release_date >")
        assertTrue(upperBound in before..after)
        val twoYears = 2 * 365L * 24 * 60 * 60
        assertTrue(lowerBound in (before - twoYears)..(after - twoYears))
    }

    @Test
    fun `the query sorts by rating count and takes up to 500 games`() = runTest {
        val query = capturedBody()

        assertTrue(query.contains("sort total_rating_count desc;"))
        assertTrue(query.contains("limit 500;"))
    }

    @Test
    fun `the query carries no platform clause`() = runTest {
        assertFalse(capturedBody().contains("platforms ="))
    }

    @Test
    fun `the rows come back in the order the service returned them`() = runTest {
        coEvery { apiService.searchGames(any<String>()) } returns listOf(igdbGame(9), igdbGame(2), igdbGame(5))

        val result = repository.getPopularGamesByGenre(12)

        assertEquals(AppResult.success(listOf(9, 2, 5)), result.map { games -> games.map { it.id } })
    }

    @Test
    fun `an HTTP failure comes back as a failure result`() = runTest {
        coEvery { apiService.searchGames(any<String>()) } throws IgdbHttpException(500, "HTTP 500")

        val result = repository.getPopularGamesByGenre(12)

        assertEquals(AppResult.failure(RepositoryError.Http(500, "HTTP 500")), result)
    }

    @Test
    fun `a cancellation is rethrown and not returned as a failure`() = runTest {
        coEvery { apiService.searchGames(any<String>()) } throws CancellationException("cancelled")

        val outcome = runCatching { repository.getPopularGamesByGenre(12) }

        assertTrue(outcome.exceptionOrNull() is CancellationException)
    }
}
