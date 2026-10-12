package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.model.SearchResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [BrowseGenreUseCase]: the games come back as the repository ranked them, the facets are derived as in
 * a text search but never offer the browsed genre, and a repository failure is passed through untouched.
 */
class BrowseGenreUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = BrowseGenreUseCase(repository)

    private val rpg = Genre(12, "Role-playing (RPG)")
    private val indie = Genre(32, "Indie")
    private val pc = Platform(6, "PC")
    private val switch = Platform(130, "Switch")

    private fun game(id: Int, genres: List<Genre>, platforms: List<Platform> = emptyList()) =
        Game(id = id, name = "Game $id", genres = genres, platforms = platforms)

    private fun givenGames(vararg games: Game) {
        coEvery { repository.getPopularGamesByGenre(any()) } returns AppResult.success(games.toList())
    }

    private suspend fun browse(genreId: Int = 12): SearchResult =
        (useCase(genreId) as AppResult.Success).data

    @Test
    fun `the games are the repository's list in the repository's order`() = runTest {
        givenGames(game(9, listOf(rpg)), game(2, listOf(rpg)), game(5, listOf(rpg)))

        assertEquals(listOf(9, 2, 5), browse().games.map { it.id })
    }

    @Test
    fun `platforms and genres are derived as in a text search`() = runTest {
        givenGames(
            game(1, listOf(rpg, indie), listOf(pc, switch)),
            game(2, listOf(rpg), listOf(switch))
        )

        assertEquals(listOf(switch, pc), browse().platforms)
    }

    @Test
    fun `the browsed genre is never offered as a genre facet`() = runTest {
        givenGames(game(1, listOf(rpg, indie)), game(2, listOf(rpg)))

        assertEquals(listOf(indie), browse(genreId = 12).genres)
    }

    @Test
    fun `a repository failure is passed through unchanged`() = runTest {
        val failure = AppResult.failure(RepositoryError.NoNetwork)
        coEvery { repository.getPopularGamesByGenre(any()) } returns failure

        assertEquals(failure, useCase(12))
    }

    @Test
    fun `the repository is asked for the given genre`() = runTest {
        givenGames()

        useCase(31)

        coVerify(exactly = 1) { repository.getPopularGamesByGenre(31) }
    }
}
