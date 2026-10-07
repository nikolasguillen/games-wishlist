package com.nikolasguillen.questlog.core.domain.usecase

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Covers that [SetGameStatusUseCase] writes a status only when the game allows it: a status that needs a
 * release is dropped for an unreleased game, a pre-order status and a cleared status always go through.
 * When a game counts as released is [com.nikolasguillen.questlog.core.domain.release.GameReleaseResolverTest]'s
 * concern; these games use plainly past or future main dates.
 */
class SetGameStatusUseCaseTest {

    private val repository = mockk<GameRepository>(relaxed = true)
    private val useCase = SetGameStatusUseCase(repository)

    private val unreleased = Game(id = 1, name = "Cindergate", releaseDate = "2999-01-15")
    private val released = Game(id = 1, name = "Cindergate", releaseDate = "2000-01-15")

    @Test
    fun `a status that needs a release is not written for an unreleased game`() = runTest {
        useCase(unreleased, GameStatus.PLAYING)

        coVerify(exactly = 0) { repository.updateGameDetails(any()) }
    }

    @Test
    fun `a pre-order status is written for an unreleased game`() = runTest {
        useCase(unreleased, GameStatus.BOUGHT)

        coVerify { repository.updateGameDetails(unreleased.copy(status = GameStatus.BOUGHT)) }
    }

    @Test
    fun `clearing a status is written even for an unreleased game`() = runTest {
        val playing = unreleased.copy(status = GameStatus.PLAYING)

        useCase(playing, null)

        coVerify { repository.updateGameDetails(playing.copy(status = null)) }
    }

    @Test
    fun `a status that needs a release is written for a released game`() = runTest {
        useCase(released, GameStatus.COMPLETED)

        coVerify { repository.updateGameDetails(released.copy(status = GameStatus.COMPLETED)) }
    }
}
