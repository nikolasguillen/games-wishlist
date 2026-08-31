package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private val PLAYING = Game(id = 1, name = "Cindergate")
private val ANTICIPATED = Game(id = 2, name = "Ashborne Reverie")

/**
 * Covers [GetDiscoverFeedUseCase]: the two shelves are combined into one [DiscoverFeed] when both
 * sources succeed, a single source failure fails the whole feed, and the platforms the user picked in
 * Settings reach both shelves.
 */
class GetDiscoverFeedUseCaseTest {

    private val repository = mockk<GameRepository>()
    private val getSelectedPlatformIds = mockk<GetSelectedPlatformIdsUseCase>()

    private val useCase = GetDiscoverFeedUseCase(repository, getSelectedPlatformIds)

    @Before
    fun setUp() {
        every { getSelectedPlatformIds() } returns flowOf(emptySet())
    }

    @Test
    fun `combines both shelves when both sources succeed`() = runTest {
        coEvery { repository.getPopularGames(any()) } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.success(listOf(ANTICIPATED))

        val result = useCase()

        assertEquals(
            AppResult.success(DiscoverFeed(popular = listOf(PLAYING), upcoming = listOf(ANTICIPATED))),
            result
        )
    }

    @Test
    fun `fails the feed when the popular source fails`() = runTest {
        coEvery { repository.getPopularGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.success(listOf(ANTICIPATED))

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }

    @Test
    fun `fails the feed when the upcoming source fails`() = runTest {
        coEvery { repository.getPopularGames(any()) } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }

    @Test
    fun `passes the selected platforms to both shelves`() = runTest {
        val selection = setOf(48, 130)
        every { getSelectedPlatformIds() } returns flowOf(selection)
        coEvery { repository.getPopularGames(any()) } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames(any()) } returns AppResult.success(listOf(ANTICIPATED))

        useCase()

        coVerify { repository.getPopularGames(selection) }
        coVerify { repository.getUpcomingGames(selection) }
    }
}
