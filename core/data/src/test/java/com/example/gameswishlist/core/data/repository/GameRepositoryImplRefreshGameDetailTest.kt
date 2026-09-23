package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.local.WishlistCoverImageStorage
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.PlatformDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.relation.GameWithAllDetails
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.network.IgdbApiService
import com.example.gameswishlist.core.network.model.IgdbGame
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.refreshGameDetail]: a row saved from search or Discover is a catalogue
 * result, not a detail, and [GameEntity.detailsFetchedAt] is what tells the two apart.
 */
class GameRepositoryImplRefreshGameDetailTest {

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

    private fun gameEntity(id: Int, detailsFetchedAt: Long?) = GameEntity(
        id = id,
        name = "Cindergate",
        description = "",
        released = null,
        backgroundImage = null,
        rating = 0.0,
        metacritic = null,
        gameTypeId = GameType.MAIN_GAME.id,
        notes = "",
        priority = null,
        status = null,
        url = null,
        detailsFetchedAt = detailsFetchedAt
    )

    private fun gameWithAllDetails(id: Int, detailsFetchedAt: Long?) = GameWithAllDetails(
        game = gameEntity(id, detailsFetchedAt),
        platformRefs = emptyList(),
        genres = emptyList(),
        companyRefs = emptyList(),
        relatedGames = emptyList(),
        engines = emptyList(),
        artworks = emptyList()
    )

    private fun igdbGame(id: Int) = IgdbGame(
        id = id,
        name = "Cindergate",
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
    fun `no local row fetches from network and stamps detailsFetchedAt`() = runTest {
        coEvery { gameDao.getGameById(1) } returns null
        coEvery { gameDao.isGameInList(any(), any()) } returns false
        coEvery { apiService.getGameDetail(any()) } returns listOf(igdbGame(1))
        val saved = slot<GameEntity>()
        coEvery { gameDao.saveGame(capture(saved), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        repository.refreshGameDetail(1)

        coVerify { apiService.getGameDetail(any()) }
        assertNotNull(saved.captured.detailsFetchedAt)
    }

    @Test
    fun `a local row with no detailsFetchedAt is a catalogue result and still fetches from network`() = runTest {
        coEvery { gameDao.getGameById(1) } returns gameWithAllDetails(id = 1, detailsFetchedAt = null)
        coEvery { gameDao.isGameInList(any(), any()) } returns false
        coEvery { apiService.getGameDetail(any()) } returns listOf(igdbGame(1))

        repository.refreshGameDetail(1)

        coVerify { apiService.getGameDetail(any()) }
    }

    @Test
    fun `a local row with detailsFetchedAt already set is a real detail and skips the network`() = runTest {
        coEvery { gameDao.getGameById(1) } returns gameWithAllDetails(id = 1, detailsFetchedAt = 1_000L)
        coEvery { gameDao.isGameInList(any(), any()) } returns false

        repository.refreshGameDetail(1)

        coVerify(exactly = 0) { apiService.getGameDetail(any()) }
    }
}
