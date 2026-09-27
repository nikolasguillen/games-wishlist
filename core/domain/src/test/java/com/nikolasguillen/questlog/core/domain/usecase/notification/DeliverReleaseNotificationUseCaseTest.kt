package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.ReleaseDate
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock

private const val GAME_ID = 1
private const val PC = 6

/**
 * Covers the fire-time eligibility re-check [DeliverReleaseNotificationUseCase] applies right before
 * posting -- the safety net that lets [SyncReleaseNotificationsUseCase]'s reconciliation stay lazy.
 */
class DeliverReleaseNotificationUseCaseTest {

    private val gameRepository = mockk<GameRepository>()
    private val notifier = mockk<ReleaseNotifier>(relaxed = true)

    private val useCase = DeliverReleaseNotificationUseCase(gameRepository, notifier)

    private val today = Clock.System.now().epochSeconds
    private val farFuture = today + 400L * 24 * 3600

    private fun game(releaseDates: List<ReleaseDate> = listOf(ReleaseDate(today, PC, "PC"))) =
        Game(id = GAME_ID, name = "Silksong", releaseDates = releaseDates)

    private fun stub(
        optedIn: Boolean = true,
        saved: List<Game> = listOf(game()),
        notifiedForDate: Long? = null
    ) {
        coEvery { gameRepository.getReleaseNotificationGameIds() } returns
            flowOf(if (optedIn) setOf(GAME_ID) else emptySet())
        coEvery { gameRepository.getSavedGames() } returns flowOf(saved)
        coEvery { gameRepository.getOwnedPlatformIds() } returns flowOf(emptySet())
        coEvery { gameRepository.getReleaseNotificationDeliveredDate(GAME_ID) } returns notifiedForDate
        coEvery { gameRepository.markReleaseNotificationDelivered(any(), any()) } just Runs
    }

    @Test
    fun `posts and marks delivered for an eligible game`() = runTest {
        stub()

        val posted = useCase(GAME_ID)

        assertTrue(posted)
        coVerify(exactly = 1) { notifier.notifyReleased(GAME_ID, "Silksong") }
        coVerify(exactly = 1) { gameRepository.markReleaseNotificationDelivered(GAME_ID, today) }
    }

    @Test
    fun `declines when the game is not opted in`() = runTest {
        stub(optedIn = false)

        val posted = useCase(GAME_ID)

        assertFalse(posted)
        coVerify(exactly = 0) { notifier.notifyReleased(any(), any()) }
    }

    @Test
    fun `declines when the game is not saved`() = runTest {
        stub(saved = emptyList())

        val posted = useCase(GAME_ID)

        assertFalse(posted)
        coVerify(exactly = 0) { notifier.notifyReleased(any(), any()) }
    }

    @Test
    fun `declines when the date is imprecise`() = runTest {
        stub(saved = listOf(game(releaseDates = listOf(ReleaseDate(today, PC, "PC", DatePrecision.QUARTER)))))

        val posted = useCase(GAME_ID)

        assertFalse(posted)
        coVerify(exactly = 0) { notifier.notifyReleased(any(), any()) }
    }

    @Test
    fun `declines when the release day is still in the future`() = runTest {
        stub(saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))))

        val posted = useCase(GAME_ID)

        assertFalse(posted)
        coVerify(exactly = 0) { notifier.notifyReleased(any(), any()) }
    }

    @Test
    fun `declines when already notified for this resolved date`() = runTest {
        stub(notifiedForDate = today)

        val posted = useCase(GAME_ID)

        assertFalse(posted)
        coVerify(exactly = 0) { notifier.notifyReleased(any(), any()) }
    }
}
