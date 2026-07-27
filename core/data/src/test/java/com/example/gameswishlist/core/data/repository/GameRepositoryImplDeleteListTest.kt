package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.local.WishlistCoverImageStorage
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.network.IgdbApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GameRepositoryImplDeleteListTest {

    private val listDao = mockk<ListDao>(relaxed = true)
    private val coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = mockk<IgdbApiService>(),
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = listDao,
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        coverImageStorage = coverImageStorage
    )

    private fun listEntity(id: Long = 7L, coverImagePath: String? = null) = ListEntity(
        id = id,
        name = "RPGs to Try",
        description = "",
        coverImagePath = coverImagePath
    )

    @Test
    fun `deleteList removes the list and its cover file`() = runTest {
        val list = listEntity(coverImagePath = "/data/covers/abc.jpg")
        coEvery { listDao.getListById(7L) } returns list

        repository.deleteList(7L)

        coVerify { listDao.deleteListWithGameRefs(list) }
        coVerify { coverImageStorage.delete("/data/covers/abc.jpg") }
    }

    /**
     * An orphaned file is invisible, whereas a surviving row pointing at a deleted file would
     * render as a broken list -- so the row has to go first.
     */
    @Test
    fun `deleteList removes the row before the cover file`() = runTest {
        val list = listEntity(coverImagePath = "/data/covers/abc.jpg")
        coEvery { listDao.getListById(7L) } returns list

        repository.deleteList(7L)

        coVerifyOrder {
            listDao.deleteListWithGameRefs(list)
            coverImageStorage.delete(any())
        }
    }

    @Test
    fun `deleteList leaves storage alone when the list has no cover`() = runTest {
        val list = listEntity(coverImagePath = null)
        coEvery { listDao.getListById(7L) } returns list

        repository.deleteList(7L)

        coVerify { listDao.deleteListWithGameRefs(list) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }

    @Test
    fun `deleteList is a no-op for an unknown list`() = runTest {
        coEvery { listDao.getListById(404L) } returns null

        repository.deleteList(404L)

        coVerify(exactly = 0) { listDao.deleteListWithGameRefs(any()) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }
}
