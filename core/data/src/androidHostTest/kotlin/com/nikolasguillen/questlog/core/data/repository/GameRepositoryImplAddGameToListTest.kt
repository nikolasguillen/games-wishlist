package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.ReleaseNotificationDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.network.IgdbApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.addGameToList]: a list shows its games through an inner join on the games
 * table, so a cross-ref written for a game that was never stored would be invisible. The catalogue [Game]
 * handed in by Search or Discover must therefore be stored first -- and an already stored row, which a
 * detail fetch may have filled, must be left exactly as it is.
 */
class GameRepositoryImplAddGameToListTest {

    private val gameDao = mockk<GameDao>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = mockk<IgdbApiService>(relaxed = true),
        gameDao = gameDao,
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        discoverCacheDao = mockk<DiscoverCacheDao>(relaxed = true),
        releaseNotificationDao = mockk<ReleaseNotificationDao>(relaxed = true),
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    private val game = Game(id = 1, name = "Cindergate")
    private val listId = 9L

    @Test
    fun `adding a game absent from storage saves it and adds the list cross-ref`() = runTest {
        coEvery { gameDao.gameExists(1) } returns false

        repository.addGameToList(game, listId)

        coVerify { gameDao.saveGame(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { gameDao.insertGameListCrossRef(GameListCrossRef(1, listId)) }
    }

    @Test
    fun `adding a game already stored does not overwrite it, but still adds the list cross-ref`() = runTest {
        coEvery { gameDao.gameExists(1) } returns true

        repository.addGameToList(game, listId)

        coVerify(exactly = 0) {
            gameDao.saveGame(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify { gameDao.insertGameListCrossRef(GameListCrossRef(1, listId)) }
    }
}
