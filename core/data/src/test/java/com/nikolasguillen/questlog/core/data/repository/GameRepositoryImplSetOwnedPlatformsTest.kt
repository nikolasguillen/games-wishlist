package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.network.IgdbApiService
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Covers [GameRepositoryImpl.setOwnedPlatforms]: a platform-selection change must clear the whole
 * Discover lane cache **before** the new selection is applied, so a cached lane can never be served for
 * a platform selection it was not fetched under (FR-002, research.md D5).
 */
class GameRepositoryImplSetOwnedPlatformsTest {

    private val platformDao = mockk<PlatformDao>(relaxed = true)
    private val discoverCacheDao = mockk<DiscoverCacheDao>(relaxed = true)

    private val repository = GameRepositoryImpl(
        apiService = mockk<IgdbApiService>(relaxed = true),
        gameDao = mockk<GameDao>(relaxed = true),
        listDao = mockk<ListDao>(relaxed = true),
        platformDao = platformDao,
        searchHistoryDao = mockk<SearchHistoryDao>(relaxed = true),
        discoverCacheDao = discoverCacheDao,
        coverImageStorage = mockk<WishlistCoverImageStorage>(relaxed = true)
    )

    @Test
    fun `the lane cache is cleared before the new platform selection is applied`() = runTest {
        repository.setOwnedPlatforms(setOf(48, 130))

        coVerifyOrder {
            discoverCacheDao.clearAll()
            platformDao.setOwnedPlatforms(setOf(48, 130))
        }
    }
}
