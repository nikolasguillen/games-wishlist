package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.ReleaseNotificationDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.domain.model.CoverImageUpdate
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.network.IgdbApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.updateList]: the three cover-image cases (keep, remove, replace), the order of
 * the row write and the old file's deletion, the failed-copy fallback that keeps the previous cover, the
 * no-op on an unknown list, and that an edit is always an `@Update` and never an `insertList` -- the
 * `REPLACE` strategy would delete and re-insert the row, which the `default_wishlist` foreign key refuses
 * for the default list.
 */
class GameRepositoryImplUpdateListTest {

    private val listDao = mockk<ListDao>(relaxed = true)
    private val coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = mockk<IgdbApiService>(),
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = listDao,
        platformDao = mockk<PlatformDao>(relaxed = true),
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        discoverCacheDao = mockk<DiscoverCacheDao>(relaxed = true),
        releaseNotificationDao = mockk<ReleaseNotificationDao>(relaxed = true),
        coverImageStorage = coverImageStorage
    )

    private fun listEntity(coverImagePath: String? = null) = ListEntity(
        id = 7L,
        name = "RPGs to Try",
        description = "Old description",
        icon = WishlistIcon.HEART,
        coverImagePath = coverImagePath
    )

    private suspend fun update(coverImage: CoverImageUpdate): AppResult<Unit> = repository.updateList(
        listId = 7L,
        name = "Co-op Picks",
        description = "New description",
        icon = WishlistIcon.BACKLOG,
        coverImage = coverImage
    )

    private fun edited(coverImagePath: String?) = listEntity().copy(
        name = "Co-op Picks",
        description = "New description",
        icon = WishlistIcon.BACKLOG,
        coverImagePath = coverImagePath
    )

    @Test
    fun `updateList on an unknown list changes nothing and succeeds`() = runTest {
        coEvery { listDao.getListById(7L) } returns null

        val result = update(CoverImageUpdate.Replace("content://picked"))

        assertEquals(AppResult.success(Unit), result)
        coVerify(exactly = 0) { listDao.updateList(any()) }
        coVerify(exactly = 0) { coverImageStorage.persist(any()) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }

    @Test
    fun `updateList with Keep updates the fields and leaves the cover and storage alone`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = "/data/covers/old.jpg")

        val result = update(CoverImageUpdate.Keep)

        assertEquals(AppResult.success(Unit), result)
        coVerify { listDao.updateList(edited(coverImagePath = "/data/covers/old.jpg")) }
        coVerify(exactly = 0) { coverImageStorage.persist(any()) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
        coVerify(exactly = 0) { listDao.insertList(any()) }
    }

    @Test
    fun `updateList with Remove clears the cover then deletes the old file`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = "/data/covers/old.jpg")

        val result = update(CoverImageUpdate.Remove)

        assertEquals(AppResult.success(Unit), result)
        coVerifyOrder {
            listDao.updateList(edited(coverImagePath = null))
            coverImageStorage.delete("/data/covers/old.jpg")
        }
        coVerify(exactly = 0) { listDao.insertList(any()) }
    }

    @Test
    fun `updateList with Remove on a list without a cover deletes nothing`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = null)

        val result = update(CoverImageUpdate.Remove)

        assertEquals(AppResult.success(Unit), result)
        coVerify { listDao.updateList(edited(coverImagePath = null)) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }

    /**
     * An orphaned file is invisible, whereas a surviving row pointing at a deleted file would render as a
     * broken list -- so the row has to be written first.
     */
    @Test
    fun `updateList with Replace stores the new path then deletes the old file`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = "/data/covers/old.jpg")
        coEvery { coverImageStorage.persist("content://picked") } returns "/data/covers/new.jpg"

        val result = update(CoverImageUpdate.Replace("content://picked"))

        assertEquals(AppResult.success(Unit), result)
        coVerifyOrder {
            listDao.updateList(edited(coverImagePath = "/data/covers/new.jpg"))
            coverImageStorage.delete("/data/covers/old.jpg")
        }
        coVerify(exactly = 0) { listDao.insertList(any()) }
    }

    @Test
    fun `updateList with Replace on a list without a cover deletes nothing`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = null)
        coEvery { coverImageStorage.persist("content://picked") } returns "/data/covers/new.jpg"

        val result = update(CoverImageUpdate.Replace("content://picked"))

        assertEquals(AppResult.success(Unit), result)
        coVerify { listDao.updateList(edited(coverImagePath = "/data/covers/new.jpg")) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }

    @Test
    fun `updateList with a Replace that fails to persist saves the fields and keeps the old cover`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = "/data/covers/old.jpg")
        coEvery { coverImageStorage.persist("content://picked") } returns null

        val result = update(CoverImageUpdate.Replace("content://picked"))

        assertEquals(AppResult.failure(RepositoryError.FileStorage), result)
        coVerify { listDao.updateList(edited(coverImagePath = "/data/covers/old.jpg")) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
        coVerify(exactly = 0) { listDao.insertList(any()) }
    }

    @Test
    fun `updateList with a failing Replace on a list without a cover still reports the failure`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity(coverImagePath = null)
        coEvery { coverImageStorage.persist("content://picked") } returns null

        val result = update(CoverImageUpdate.Replace("content://picked"))

        assertEquals(AppResult.failure(RepositoryError.FileStorage), result)
        coVerify { listDao.updateList(edited(coverImagePath = null)) }
        coVerify(exactly = 0) { coverImageStorage.delete(any()) }
    }

    @Test
    fun `updateList keeps the list's identity`() = runTest {
        coEvery { listDao.getListById(7L) } returns listEntity()

        update(CoverImageUpdate.Keep)

        coVerify { listDao.updateList(match { it.id == 7L }) }
    }
}
