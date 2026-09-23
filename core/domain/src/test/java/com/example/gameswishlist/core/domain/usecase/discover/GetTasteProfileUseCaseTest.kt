package com.example.questlog.core.domain.usecase.discover

import com.example.questlog.core.domain.repository.GameRepository
import com.example.questlog.core.model.Company
import com.example.questlog.core.model.Engine
import com.example.questlog.core.model.Game
import com.example.questlog.core.model.GameStatus
import com.example.questlog.core.model.Genre
import com.example.questlog.core.model.Priority
import com.example.questlog.core.model.TasteProfile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val RPG = 1
private const val SHOOTER = 2
private const val LARIAN = 10
private const val FROM_SOFTWARE = 11

class GetTasteProfileUseCaseTest {

    private val repository = mockk<GameRepository>()

    private val useCase = GetTasteProfileUseCase(repository)

    private fun game(
        id: Int,
        genreIds: List<Int> = listOf(RPG),
        developerIds: List<Int> = listOf(LARIAN),
        publisherIds: List<Int> = emptyList(),
        engineIds: List<Int> = emptyList(),
        status: GameStatus? = null,
        priority: Priority? = null,
        lastViewedAt: Long? = null
    ) = Game(
        id = id,
        name = "Game $id",
        genres = genreIds.map { Genre(it, "Genre $it") },
        developers = developerIds.map { Company(it, "Developer $it") },
        publishers = publisherIds.map { Company(it, "Publisher $it") },
        engines = engineIds.map { Engine(it, "Engine $it") },
        status = status,
        priority = priority,
        lastViewedAt = lastViewedAt
    )

    private fun givenSavedGames(vararg games: Game) {
        every { repository.getSavedGames() } returns flowOf(games.toList())
    }

    @Test
    fun `an empty library produces the empty profile`() = runTest {
        givenSavedGames()

        val profile = useCase().first()

        assertEquals(TasteProfile.EMPTY, profile)
        assertTrue(profile.isEmpty)
    }

    @Test
    fun `a completed game gives its genre and developer a positive weight and a count of one`() = runTest {
        givenSavedGames(game(id = 1, status = GameStatus.COMPLETED))

        val profile = useCase().first()

        assertEquals(1.0, profile.genres.getValue(RPG).weight, 0.0001)
        assertEquals(1.0, profile.developers.getValue(LARIAN).weight, 0.0001)
        assertEquals(1, profile.developers.getValue(LARIAN).count)
        assertEquals(1, profile.sampleSize)
    }

    /**
     * The point of tracking [GameStatus.DROPPED] at all: a rejection has to be able to push a genre
     * below zero, otherwise a genre the user keeps bouncing off still reads as a preference.
     */
    @Test
    fun `a dropped game pushes its genre negative`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.COMPLETED),
            game(id = 2, genreIds = listOf(SHOOTER), status = GameStatus.DROPPED)
        )

        val profile = useCase().first()

        assertTrue(profile.genres.getValue(SHOOTER).weight < 0.0)
        assertTrue(profile.genres.getValue(RPG).weight > 0.0)
    }

    /**
     * A dropped game is a rejection, not a recurrence: it must not inflate the count of the developer
     * or genre it carried, even though it still moves the weight (see the test above).
     */
    @Test
    fun `a dropped game does not count towards its developer's recurrence`() = runTest {
        givenSavedGames(
            game(id = 1, developerIds = listOf(LARIAN), status = GameStatus.DROPPED)
        )

        val profile = useCase().first()

        assertEquals(0, profile.developers.getValue(LARIAN).count)
    }

    /**
     * Two saved games from the same studio is the signal the "More from <studio>" shelf keys off of.
     */
    @Test
    fun `two saved games from the same developer give it a count of two`() = runTest {
        givenSavedGames(
            game(id = 1, developerIds = listOf(LARIAN), status = GameStatus.COMPLETED),
            game(id = 2, developerIds = listOf(LARIAN), status = GameStatus.PLAYING)
        )

        val profile = useCase().first()

        assertEquals(2, profile.developers.getValue(LARIAN).count)
    }

    /**
     * Weight and count answer different questions and must not be conflated: a single high-priority
     * completed game can outweigh two low-priority want-to-buy games from a different developer, while
     * still recurring less often.
     */
    @Test
    fun `weight and count rank developers independently`() = runTest {
        givenSavedGames(
            game(id = 1, developerIds = listOf(LARIAN), status = GameStatus.COMPLETED, priority = Priority.HIGH),
            game(id = 2, developerIds = listOf(FROM_SOFTWARE), status = GameStatus.WANT_TO_BUY, priority = Priority.LOW),
            game(id = 3, developerIds = listOf(FROM_SOFTWARE), status = GameStatus.WANT_TO_BUY, priority = Priority.LOW)
        )

        val profile = useCase().first()

        assertTrue(profile.developers.getValue(LARIAN).weight > profile.developers.getValue(FROM_SOFTWARE).weight)
        assertTrue(profile.developers.getValue(LARIAN).count < profile.developers.getValue(FROM_SOFTWARE).count)
    }

    /**
     * A genre the user completed once and dropped once is not a preference. It has to land at zero
     * rather than at the average of two positives. The third game only exists to give the map a
     * non-zero peak to normalise against.
     */
    @Test
    fun `completing and dropping the same genre cancels out`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), developerIds = listOf(LARIAN), status = GameStatus.COMPLETED),
            game(id = 2, genreIds = listOf(RPG), developerIds = listOf(FROM_SOFTWARE), status = GameStatus.DROPPED),
            game(id = 3, genreIds = listOf(SHOOTER), status = GameStatus.COMPLETED)
        )

        val profile = useCase().first()

        assertEquals(0.0, profile.genres.getValue(RPG).weight, 0.0001)
    }

    /**
     * Every signal cancelling exactly is not a preference for anything, and there is no peak to
     * divide by — the profile reports no genre signal rather than a map of zeroes. [TasteProfile.sampleSize]
     * still counts the games, so this is distinguishable from a cold start.
     */
    @Test
    fun `a library where everything cancels reports no genre signal`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.COMPLETED),
            game(id = 2, genreIds = listOf(RPG), status = GameStatus.DROPPED)
        )

        val profile = useCase().first()

        assertTrue(profile.genres.isEmpty())
        assertEquals(2, profile.sampleSize)
        assertTrue(!profile.isEmpty)
    }

    @Test
    fun `high priority outweighs low priority for the same status`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.PLAYING, priority = Priority.HIGH),
            game(id = 2, genreIds = listOf(SHOOTER), status = GameStatus.PLAYING, priority = Priority.LOW)
        )

        val profile = useCase().first()

        assertTrue(profile.genres.getValue(RPG).weight > profile.genres.getValue(SHOOTER).weight)
    }

    /**
     * Wanting to buy something is intent, not taste — it must not count as much as having actually
     * finished a game.
     */
    @Test
    fun `want to buy counts for less than completed`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.COMPLETED),
            game(id = 2, genreIds = listOf(SHOOTER), status = GameStatus.WANT_TO_BUY)
        )

        val profile = useCase().first()

        assertTrue(profile.genres.getValue(SHOOTER).weight < profile.genres.getValue(RPG).weight)
    }

    @Test
    fun `publishers and engines contribute nothing`() = runTest {
        givenSavedGames(
            game(
                id = 1,
                developerIds = listOf(LARIAN),
                publisherIds = listOf(FROM_SOFTWARE),
                engineIds = listOf(99),
                status = GameStatus.COMPLETED
            )
        )

        val profile = useCase().first()

        assertEquals(setOf(LARIAN), profile.developers.keys)
    }

    /**
     * Recency is a tie-break, so it may reorder two games the explicit signals scored the same, but
     * it must never let a merely-viewed game outrank a completed one.
     */
    @Test
    fun `recently viewed breaks a tie without overturning status`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.PLAYING, lastViewedAt = 1_000L),
            game(id = 2, genreIds = listOf(SHOOTER), status = GameStatus.PLAYING),
            game(id = 3, genreIds = listOf(3), status = GameStatus.WANT_TO_BUY, lastViewedAt = 2_000L)
        )

        val profile = useCase().first()

        assertTrue(profile.genres.getValue(RPG).weight > profile.genres.getValue(SHOOTER).weight)
        assertTrue(profile.genres.getValue(3).weight < profile.genres.getValue(SHOOTER).weight)
    }

    @Test
    fun `weights stay within the normalised range`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.COMPLETED, priority = Priority.HIGH),
            game(id = 2, genreIds = listOf(RPG), status = GameStatus.PLAYING, priority = Priority.HIGH),
            game(id = 3, genreIds = listOf(SHOOTER), status = GameStatus.DROPPED, priority = Priority.HIGH)
        )

        val profile = useCase().first()

        assertEquals(1.0, profile.genres.getValue(RPG).weight, 0.0001)
        val allWeights = profile.genres.values.map { it.weight } + profile.developers.values.map { it.weight }
        assertTrue(allWeights.all { it >= -1.0 && it <= 1.0 })
    }

    /**
     * A game saved to a list without ever being classified is still a choice the user made, so it
     * counts — just less than one they played.
     */
    @Test
    fun `a game with no status still contributes`() = runTest {
        givenSavedGames(
            game(id = 1, genreIds = listOf(RPG), status = GameStatus.COMPLETED),
            game(id = 2, genreIds = listOf(SHOOTER), status = null)
        )

        val profile = useCase().first()

        val shooter = profile.genres.getValue(SHOOTER).weight
        assertTrue(shooter > 0.0)
        assertTrue(shooter < profile.genres.getValue(RPG).weight)
    }
}
