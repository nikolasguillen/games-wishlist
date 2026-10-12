package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.Priority
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [GetBrowseGenresUseCase], the five chips of Search's "Browse by genre" row: taste genres first
 * (positive weight only, strongest first, ties broken the same way every time), padded from the fixed
 * defaults without duplicates, never fewer than five, and re-emitting only when the row itself changes.
 */
class GetBrowseGenresUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetBrowseGenresUseCase(repository)

    private val defaultIds = listOf(12, 5, 31, 15, 32)

    private fun game(
        id: Int,
        genreIds: List<Int>,
        status: GameStatus? = GameStatus.PLAYING,
        priority: Priority? = null
    ) = Game(
        id = id,
        name = "Game $id",
        genres = genreIds.map { Genre(it, "Genre $it") },
        status = status,
        priority = priority
    )

    private fun givenSavedGames(vararg games: Game) {
        every { repository.getSavedGames() } returns flowOf(games.toList())
    }

    private suspend fun ids(): List<Int> = useCase().first().map { it.id }

    @Test
    fun `no saved games gives the five defaults in order`() = runTest {
        givenSavedGames()

        assertEquals(defaultIds, ids())
    }

    @Test
    fun `the defaults carry IGDB's name for each genre`() = runTest {
        givenSavedGames()

        assertEquals(
            listOf("Role-playing (RPG)", "Shooter", "Adventure", "Strategy", "Indie"),
            useCase().first().map { it.name }
        )
    }

    @Test
    fun `taste genres lead and the defaults fill the rest without duplicates`() = runTest {
        givenSavedGames(game(1, genreIds = listOf(100, 101, 102)))

        assertEquals(listOf(100, 101, 102, 12, 5), ids())
    }

    @Test
    fun `more than five positive genres keep only the five strongest and no default`() = runTest {
        givenSavedGames(
            game(1, listOf(1), status = GameStatus.PLAYING),
            game(2, listOf(2), status = GameStatus.COMPLETED),
            game(3, listOf(3), status = GameStatus.BOUGHT),
            game(4, listOf(4), status = null),
            game(5, listOf(5), status = GameStatus.WANT_TO_BUY),
            game(6, listOf(6), status = GameStatus.WANT_TO_BUY, priority = Priority.LOW)
        )

        assertEquals(listOf(1, 2, 3, 4, 5), ids())
    }

    @Test
    fun `a taste genre that is also a default appears once, at its taste position`() = runTest {
        givenSavedGames(game(1, genreIds = listOf(31)))

        assertEquals(listOf(31, 12, 5, 15, 32), ids())
    }

    @Test
    fun `a genre only dropped games carry is not offered`() = runTest {
        givenSavedGames(
            game(1, genreIds = listOf(100), status = GameStatus.PLAYING),
            game(2, genreIds = listOf(200), status = GameStatus.DROPPED)
        )

        assertEquals(listOf(100, 12, 5, 31, 15), ids())
    }

    @Test
    fun `a tie on weight is broken by how many games back it, then by ascending id`() = runTest {
        // 40 and 50 both score 1.0, but 50 gets there through two games: it comes first although its id is
        // higher. 60 and 70 tie on everything, so the lower id wins.
        givenSavedGames(
            game(1, genreIds = listOf(40), status = GameStatus.PLAYING),
            game(2, genreIds = listOf(50), status = GameStatus.BOUGHT),
            game(3, genreIds = listOf(50), status = null),
            game(4, genreIds = listOf(70), status = GameStatus.PLAYING),
            game(5, genreIds = listOf(60), status = GameStatus.PLAYING)
        )

        val first = ids()
        val second = ids()

        assertEquals(listOf(50, 40, 60, 70, 12), first)
        assertEquals(first, second)
    }

    @Test
    fun `every emission has exactly five distinct genres`() = runTest {
        every { repository.getSavedGames() } returns flowOf(
            emptyList(),
            listOf(game(1, genreIds = listOf(12))),
            listOf(game(1, genreIds = listOf(12, 5, 31, 15, 32, 100, 101)))
        )

        val emissions = useCase().toList()

        emissions.forEach { row ->
            assertEquals(5, row.size)
            assertEquals(5, row.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `a library change that leaves the row alone emits nothing, one that changes it emits`() = runTest {
        every { repository.getSavedGames() } returns flowOf(
            emptyList(),
            emptyList(),
            listOf(game(1, genreIds = listOf(100)))
        )

        val emissions = useCase().toList().map { row -> row.map { it.id } }

        assertEquals(listOf(defaultIds, listOf(100, 12, 5, 31, 15)), emissions)
    }

    @Test
    fun `a saved-games flow that throws gives the five defaults instead of failing`() = runTest {
        val broken: Flow<List<Game>> = flow { throw IllegalStateException("database unreadable") }
        every { repository.getSavedGames() } returns broken

        val emissions = useCase().toList().map { row -> row.map { it.id } }

        assertEquals(listOf(defaultIds), emissions)
    }
}
