package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers [GetDeveloperGamesUseCase]: newest first with the undated games last, and a failure passed through. */
class GetDeveloperGamesUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetDeveloperGamesUseCase(repository)

    private fun game(id: Int, releaseDate: String?) = Game(id = id, name = "Game $id", releaseDate = releaseDate)

    @Test
    fun `games are newest first and undated games are last`() = runTest {
        coEvery { repository.getDevelopedGames(401) } returns AppResult.success(
            listOf(game(1, null), game(2, "1999-01-01"), game(3, "2024-06-30"))
        )

        val games = (useCase(401) as AppResult.Success).data

        assertEquals(listOf(3, 2, 1), games.map { it.id })
    }

    @Test
    fun `games with the same date keep the order they came in`() = runTest {
        coEvery { repository.getDevelopedGames(401) } returns AppResult.success(
            listOf(game(5, "2020-01-01"), game(3, "2020-01-01"), game(9, "2020-01-01"))
        )

        val games = (useCase(401) as AppResult.Success).data

        assertEquals(listOf(5, 3, 9), games.map { it.id })
    }

    @Test
    fun `a failure passes through`() = runTest {
        coEvery { repository.getDevelopedGames(401) } returns AppResult.failure(RepositoryError.NoNetwork)

        assertEquals(AppResult.failure(RepositoryError.NoNetwork), useCase(401))
    }
}
