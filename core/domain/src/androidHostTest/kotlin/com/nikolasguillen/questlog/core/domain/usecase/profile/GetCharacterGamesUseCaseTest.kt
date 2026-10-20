package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GetCharacterGamesUseCase]: the games come back newest first with the undated ones last, an empty id
 * list makes no repository call, and a failure passes through.
 */
class GetCharacterGamesUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetCharacterGamesUseCase(repository)

    private fun game(id: Int, releaseDate: String?) = Game(id = id, name = "Game $id", releaseDate = releaseDate)

    @Test
    fun `games are newest first and undated games are last`() = runTest {
        coEvery { repository.getGamesByIds(listOf(1, 2, 3, 4)) } returns AppResult.success(
            listOf(game(1, "2005-03-22"), game(2, null), game(3, "2018-04-20"), game(4, "2013-03-12"))
        )

        val games = (useCase(listOf(1, 2, 3, 4)) as AppResult.Success).data

        assertEquals(listOf(3, 4, 1, 2), games.map { it.id })
    }

    @Test
    fun `an empty id list is an empty success with no repository call`() = runTest {
        val result = useCase(emptyList())

        assertEquals(AppResult.success(emptyList<Game>()), result)
        coVerify(exactly = 0) { repository.getGamesByIds(any()) }
    }

    @Test
    fun `a failure passes through`() = runTest {
        coEvery { repository.getGamesByIds(any()) } returns AppResult.failure(RepositoryError.RequestTimeout)

        assertEquals(AppResult.failure(RepositoryError.RequestTimeout), useCase(listOf(1)))
    }
}
