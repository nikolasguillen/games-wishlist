package com.example.gameswishlist.core.domain.usecase.discover

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.DiscoverFeed
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val PLAYING = Game(id = 1, name = "Cindergate")
private val ANTICIPATED = Game(id = 2, name = "Ashborne Reverie")

/**
 * Covers [GetDiscoverFeedUseCase]: the two shelves are combined into one [DiscoverFeed] when both
 * sources succeed, and a single source failure fails the whole feed.
 */
class GetDiscoverFeedUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetDiscoverFeedUseCase(repository)

    @Test
    fun `combines both shelves when both sources succeed`() = runTest {
        coEvery { repository.getPopularGames() } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames() } returns AppResult.success(listOf(ANTICIPATED))

        val result = useCase()

        assertEquals(
            AppResult.success(DiscoverFeed(popular = listOf(PLAYING), upcoming = listOf(ANTICIPATED))),
            result
        )
    }

    @Test
    fun `fails the feed when the popular source fails`() = runTest {
        coEvery { repository.getPopularGames() } returns AppResult.failure(RepositoryError.NoNetwork)
        coEvery { repository.getUpcomingGames() } returns AppResult.success(listOf(ANTICIPATED))

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }

    @Test
    fun `fails the feed when the upcoming source fails`() = runTest {
        coEvery { repository.getPopularGames() } returns AppResult.success(listOf(PLAYING))
        coEvery { repository.getUpcomingGames() } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase())
    }
}
