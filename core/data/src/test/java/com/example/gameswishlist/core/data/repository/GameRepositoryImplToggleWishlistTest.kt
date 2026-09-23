package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.local.WishlistCoverImageStorage
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.PlatformDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.network.IgdbApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.toggleWishlist]: a catalogue [Game] (from search or Discover, carrying none
 * of the user's own fields or per-platform detail) must never overwrite a row already in storage.
 */
class GameRepositoryImplToggleWishlistTest {

    private val gameDao = mockk<GameDao>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = mockk<IgdbApiService>(relaxed = true),
        gameDao = gameDao,
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    private val game = Game(id = 1, name = "Cindergate")

    @Test
    fun `adding a game absent from storage saves it and adds the list cross-ref`() = runTest {
        coEvery { gameDao.isGameInList(any(), any()) } returns false
        coEvery { gameDao.gameExists(1) } returns false

        repository.toggleWishlist(game)

        coVerify { gameDao.saveGame(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { gameDao.insertGameListCrossRef(any()) }
    }

    @Test
    fun `adding a game already stored does not overwrite it, but still adds the list cross-ref`() = runTest {
        coEvery { gameDao.isGameInList(any(), any()) } returns false
        coEvery { gameDao.gameExists(1) } returns true

        repository.toggleWishlist(game)

        coVerify(exactly = 0) {
            gameDao.saveGame(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify { gameDao.insertGameListCrossRef(any()) }
    }
}
