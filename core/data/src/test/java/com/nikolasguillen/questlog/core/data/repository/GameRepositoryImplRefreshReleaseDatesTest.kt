package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.entity.GamePlatformCrossRef
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.model.IgdbPlatform
import com.nikolasguillen.questlog.core.network.model.IgdbReleaseDateEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.refreshSavedGameReleaseDates]: chunking saved-game ids into requests of at
 * most 20 (one game can contribute several platform x region rows and the endpoint caps at 500), and that
 * the fetched entries are written through [GameDao.upsertGamePlatformCrossRefs] and
 * [GameDao.insertPlatformIfAbsent].
 */
class GameRepositoryImplRefreshReleaseDatesTest {

    private val apiService = mockk<IgdbApiService>()
    private val gameDao = mockk<GameDao>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = apiService,
        gameDao = gameDao,
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    private fun RequestBody.asText(): String = Buffer().also { writeTo(it) }.readUtf8()

    private fun idsInBody(body: RequestBody): List<String> =
        Regex("""game = \(([^)]*)\)""").find(body.asText())!!.groupValues[1].split(",")

    @Test
    fun `45 saved game ids are chunked into 3 requests of at most 20`() = runTest {
        coEvery { gameDao.getSavedGameIds() } returns (1..45).toList()
        val bodies = mutableListOf<RequestBody>()
        coEvery { apiService.getReleaseDates(capture(bodies)) } returns emptyList()

        repository.refreshSavedGameReleaseDates()

        coVerify(exactly = 3) { apiService.getReleaseDates(any()) }
        assertEquals(listOf(5, 20, 20), bodies.map { idsInBody(it).size }.sorted())
        // No id is skipped or sent twice across the chunks, regardless of dispatch order.
        assertEquals((1..45).map { it.toString() }.toSet(), bodies.flatMap { idsInBody(it) }.toSet())
    }

    @Test
    fun `no saved games means no network call and an immediate success`() = runTest {
        coEvery { gameDao.getSavedGameIds() } returns emptyList()

        repository.refreshSavedGameReleaseDates()

        coVerify(exactly = 0) { apiService.getReleaseDates(any()) }
    }

    @Test
    fun `fetched entries are upserted and their platforms backfilled`() = runTest {
        coEvery { gameDao.getSavedGameIds() } returns listOf(100)
        coEvery { apiService.getReleaseDates(any()) } returns listOf(
            IgdbReleaseDateEntry(
                id = 1,
                game = 100,
                platform = IgdbPlatform(
                    id = 6, abbreviation = "PC", name = "PC", generation = null, category = null, platformFamily = null
                ),
                date = 1_000L,
                dateFormat = 0
            )
        )
        val upserted = slot<List<GamePlatformCrossRef>>()
        coEvery { gameDao.upsertGamePlatformCrossRefs(capture(upserted)) } returns Unit

        repository.refreshSavedGameReleaseDates()

        coVerify { gameDao.insertPlatformIfAbsent(match { it.id == 6 }) }
        assertEquals(
            GamePlatformCrossRef(gameId = 100, platformId = 6, releaseDate = 1_000L, releaseDatePrecision = 0),
            upserted.captured.single()
        )
    }
}
