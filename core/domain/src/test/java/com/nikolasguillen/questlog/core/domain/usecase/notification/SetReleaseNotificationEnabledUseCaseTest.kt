package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val GAME_ID = 1

/** Covers the single entry point behind every "Notify me" bell: write, then reconcile, in that order. */
class SetReleaseNotificationEnabledUseCaseTest {

    private val gameRepository = mockk<GameRepository>()
    private val syncReleaseNotificationsUseCase = mockk<SyncReleaseNotificationsUseCase>(relaxed = true)

    private val useCase = SetReleaseNotificationEnabledUseCase(gameRepository, syncReleaseNotificationsUseCase)

    @Test
    fun `enabling writes the opt-in then reconciles that game`() = runTest {
        coEvery { gameRepository.setReleaseNotificationEnabled(GAME_ID, true) } just Runs

        useCase(GAME_ID, true)

        coVerifyOrder {
            gameRepository.setReleaseNotificationEnabled(GAME_ID, true)
            syncReleaseNotificationsUseCase(GAME_ID)
        }
    }

    @Test
    fun `disabling clears the opt-in then reconciles (cancelling) that game`() = runTest {
        coEvery { gameRepository.setReleaseNotificationEnabled(GAME_ID, false) } just Runs

        useCase(GAME_ID, false)

        coVerifyOrder {
            gameRepository.setReleaseNotificationEnabled(GAME_ID, false)
            syncReleaseNotificationsUseCase(GAME_ID)
        }
    }
}
