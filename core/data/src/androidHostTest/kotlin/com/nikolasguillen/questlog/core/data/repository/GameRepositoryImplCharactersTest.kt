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
import com.nikolasguillen.questlog.core.network.model.IgdbCharacter
import com.nikolasguillen.questlog.core.network.model.IgdbImage
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the character half of [GameRepositoryImpl]: the search and profile queries it sends to `/characters`,
 * how the rows map, and how a failure or a missing row is reported.
 */
class GameRepositoryImplCharactersTest {

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

    @Test
    fun `a character search sends the text search and asks for the name and the portrait only`() = runTest {
        val body = slot<String>()
        coEvery { apiService.getCharacters(capture(body)) } returns listOf(
            IgdbCharacter(id = 7, name = "Kratos", mugShot = IgdbImage(1, "//images.igdb.com/t_thumb/a.jpg"))
        )

        val result = repository.searchCharacters("kratos")

        assertTrue(body.captured.contains("search \"kratos\";"))
        assertTrue(body.captured.contains("fields name, mug_shot.url;"))
        assertTrue(body.captured.contains("limit 100;"))
        val characters = (result as AppResult.Success).data
        assertEquals("Kratos", characters.single().name)
        assertEquals("https://images.igdb.com/t_cover_big/a.jpg", characters.single().imageUrl)
    }

    @Test
    fun `a character profile asks for the description and the game ids of that character`() = runTest {
        val body = slot<String>()
        coEvery { apiService.getCharacters(capture(body)) } returns listOf(
            IgdbCharacter(id = 7, name = "Kratos", description = "  ", games = listOf(1, 2))
        )

        val result = repository.getCharacterProfile(7)

        assertTrue(body.captured.contains("fields name, description, mug_shot.url, games;"))
        assertTrue(body.captured.contains("where id = 7;"))
        val profile = (result as AppResult.Success).data
        assertNull(profile.description)
        assertEquals(listOf(1, 2), profile.gameIds)
    }

    @Test
    fun `a character profile with no row is an unknown failure`() = runTest {
        coEvery { apiService.getCharacters(any()) } returns emptyList()

        val result = repository.getCharacterProfile(7)

        assertTrue((result as AppResult.Failure).error is RepositoryError.Unknown)
    }

    @Test
    fun `a failed character search is mapped to a repository error`() = runTest {
        coEvery { apiService.getCharacters(any()) } throws IgdbHttpException(code = 429, message = "Too Many Requests")

        val result = repository.searchCharacters("kratos")

        assertEquals(RepositoryError.Http(429, "Too Many Requests"), (result as AppResult.Failure).error)
    }
}
