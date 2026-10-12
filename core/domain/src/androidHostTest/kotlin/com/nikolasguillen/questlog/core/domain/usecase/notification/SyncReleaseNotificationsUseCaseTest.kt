package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder
import com.nikolasguillen.questlog.core.domain.radar.resolveNotificationInstant
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.ReleaseDate
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Clock

private const val GAME_ID = 1
private const val OTHER_GAME_ID = 2
private const val PC = 6
private const val PS5 = 167

/**
 * Covers the eligibility predicate from data-model.md as applied by [SyncReleaseNotificationsUseCase]:
 * scheduling an eligible game, declining a coarse or elapsed date, re-arming after a date shift, pruning
 * an opt-in that left the saved set, and the earliest-of-multiple-platforms rule (FR-015).
 */
class SyncReleaseNotificationsUseCaseTest {

    private val gameRepository = mockk<GameRepository>()
    private val scheduler = mockk<ReleaseNotificationScheduler>(relaxed = true).also {
        coEvery { it.takeFiredReminders() } returns emptyMap()
    }

    private val useCase = SyncReleaseNotificationsUseCase(gameRepository, scheduler)

    private val farFuture = Clock.System.now().epochSeconds + 400L * 24 * 3600
    private val past = Clock.System.now().epochSeconds - 400L * 24 * 3600

    private fun game(id: Int = GAME_ID, releaseDates: List<ReleaseDate> = emptyList()) =
        Game(id = id, name = "Game $id", releaseDates = releaseDates)

    private fun stub(
        optedIn: Set<Int>,
        saved: List<Game>,
        ownedPlatformIds: Set<Int> = emptySet(),
        notifiedForDate: Long? = null
    ) {
        coEvery { gameRepository.getReleaseNotificationGameIds() } returns flowOf(optedIn)
        coEvery { gameRepository.getSavedGames() } returns flowOf(saved)
        coEvery { gameRepository.getOwnedPlatformIds() } returns flowOf(ownedPlatformIds)
        coEvery { gameRepository.getReleaseNotificationDeliveredDate(any()) } returns notifiedForDate
        coEvery { gameRepository.setReleaseNotificationEnabled(any(), any()) } just Runs
        coEvery { gameRepository.markReleaseNotificationDelivered(any(), any()) } just Runs
    }

    @Test
    fun `schedules an eligible future exact-date game`() = runTest {
        stub(optedIn = setOf(GAME_ID), saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))))

        useCase()

        coVerify(exactly = 1) { scheduler.schedule(match { it.gameId == GAME_ID }) }
        coVerify(exactly = 0) { scheduler.cancel(any()) }
    }

    @Test
    fun `cancels and does not error on a coarser-precision game`() = runTest {
        listOf(DatePrecision.YEAR_MONTH, DatePrecision.QUARTER, DatePrecision.YEAR_ONLY, DatePrecision.TBD)
            .forEach { precision ->
                // A fresh mock per iteration: coVerify's call count is cumulative across a shared mock.
                val repository = mockk<GameRepository>()
                val perIterationScheduler = mockk<ReleaseNotificationScheduler>(relaxed = true)
                coEvery { repository.getReleaseNotificationGameIds() } returns flowOf(setOf(GAME_ID))
                coEvery { repository.getSavedGames() } returns
                    flowOf(listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC", precision)))))
                coEvery { repository.getOwnedPlatformIds() } returns flowOf(emptySet())
                coEvery { repository.getReleaseNotificationDeliveredDate(any()) } returns null

                SyncReleaseNotificationsUseCase(repository, perIterationScheduler).invoke()

                coVerify(exactly = 1) { perIterationScheduler.cancel(GAME_ID) }
            }
    }

    @Test
    fun `cancels on a past date`() = runTest {
        stub(optedIn = setOf(GAME_ID), saved = listOf(game(releaseDates = listOf(ReleaseDate(past, PC, "PC")))))

        useCase()

        coVerify(exactly = 1) { scheduler.cancel(GAME_ID) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `re-arms when the resolved date differs from the notified date`() = runTest {
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))),
            notifiedForDate = farFuture - 1_000
        )

        useCase()

        coVerify(exactly = 1) { scheduler.schedule(match { it.gameId == GAME_ID }) }
    }

    @Test
    fun `does not reschedule when already notified for the resolved date`() = runTest {
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))),
            notifiedForDate = farFuture
        )

        useCase()

        coVerify(exactly = 1) { scheduler.cancel(GAME_ID) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `prunes an opted-in game that left the saved set`() = runTest {
        stub(optedIn = setOf(GAME_ID), saved = emptyList())

        useCase()

        coVerify(exactly = 1) { gameRepository.setReleaseNotificationEnabled(GAME_ID, false) }
        coVerify(exactly = 1) { scheduler.cancel(GAME_ID) }
    }

    @Test
    fun `picks the earliest of several owned-platform dates`() = runTest {
        val earliest = farFuture - 100_000
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(
                game(
                    releaseDates = listOf(
                        ReleaseDate(farFuture, PC, "PC"),
                        ReleaseDate(earliest, PS5, "PS5")
                    )
                )
            ),
            ownedPlatformIds = setOf(PC, PS5)
        )
        val reminderSlot = slot<ReleaseReminder>()
        coEvery { scheduler.schedule(capture(reminderSlot)) } just Runs

        useCase()

        val expected = resolveNotificationInstant(earliest, DatePrecision.EXACT_DATE, Clock.System.now())
        assertEquals(expected, reminderSlot.captured.fireAt)
        assertEquals(earliest, reminderSlot.captured.releaseDateEpochSeconds)
        assertEquals("Game $GAME_ID", reminderSlot.captured.gameName)
    }

    @Test
    fun `narrows to one game when gameId is passed`() = runTest {
        stub(
            optedIn = setOf(GAME_ID, OTHER_GAME_ID),
            saved = listOf(
                game(id = GAME_ID, releaseDates = listOf(ReleaseDate(farFuture, PC, "PC"))),
                game(id = OTHER_GAME_ID, releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))
            )
        )

        useCase(GAME_ID)

        coVerify(exactly = 1) { scheduler.schedule(match { it.gameId == GAME_ID }) }
        coVerify(exactly = 0) { scheduler.schedule(match { it.gameId == OTHER_GAME_ID }) }
    }

    @Test
    fun `disable path cancels even though the opt-in row is already gone`() {
        // Simulates SetReleaseNotificationEnabledUseCase's disable branch: by the time Sync runs,
        // getReleaseNotificationGameIds() no longer contains gameId.
        runTest {
            stub(optedIn = emptySet(), saved = emptyList())

            useCase(GAME_ID)

            coVerify(exactly = 1) { scheduler.cancel(GAME_ID) }
            coVerify(exactly = 0) { gameRepository.setReleaseNotificationEnabled(any(), any()) }
        }
    }

    @Test
    fun `marks a fired reminder delivered before the loop so the same date is not re-armed`() = runTest {
        var deliveredDate: Long? = null
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC"))))
        )
        coEvery { scheduler.takeFiredReminders() } returns mapOf(GAME_ID to farFuture)
        coEvery { gameRepository.markReleaseNotificationDelivered(any(), any()) } answers {
            deliveredDate = secondArg()
        }
        coEvery { gameRepository.getReleaseNotificationDeliveredDate(any()) } answers { deliveredDate }

        useCase()

        coVerify(exactly = 1) { gameRepository.markReleaseNotificationDelivered(GAME_ID, farFuture) }
        coVerify(exactly = 1) { scheduler.cancel(GAME_ID) }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `does not mark a fired reminder for a game that is no longer opted in`() = runTest {
        stub(optedIn = emptySet(), saved = emptyList())
        coEvery { scheduler.takeFiredReminders() } returns mapOf(GAME_ID to farFuture)

        useCase()

        coVerify(exactly = 0) { gameRepository.markReleaseNotificationDelivered(any(), any()) }
    }

    @Test
    fun `a fired date does not stop the game being armed once its release date has moved`() = runTest {
        var deliveredDate: Long? = null
        val movedDate = farFuture + 86_400
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(movedDate, PC, "PC"))))
        )
        coEvery { scheduler.takeFiredReminders() } returns mapOf(GAME_ID to farFuture)
        coEvery { gameRepository.markReleaseNotificationDelivered(any(), any()) } answers {
            deliveredDate = secondArg()
        }
        coEvery { gameRepository.getReleaseNotificationDeliveredDate(any()) } answers { deliveredDate }

        useCase()

        coVerify(exactly = 1) { gameRepository.markReleaseNotificationDelivered(GAME_ID, farFuture) }
        coVerify(exactly = 1) { scheduler.schedule(match { it.releaseDateEpochSeconds == movedDate }) }
    }

    @Test
    fun `takes the fired reminders once per full sync and once per single-game sync`() = runTest {
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC"))))
        )

        useCase()
        useCase(GAME_ID)

        coVerify(exactly = 2) { scheduler.takeFiredReminders() }
    }

    @Test
    fun `an empty scope still collects the fired reminders`() = runTest {
        stub(optedIn = emptySet(), saved = emptyList())

        useCase()

        coVerify(exactly = 1) { scheduler.takeFiredReminders() }
    }

    @Test
    fun `overlapping syncs run one after the other and the second does not re-arm a fired date`() = runTest {
        var deliveredDate: Long? = null
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC"))))
        )
        // The first call reports the fired reminder, the second finds the ledger already drained.
        coEvery { scheduler.takeFiredReminders() } returnsMany listOf(mapOf(GAME_ID to farFuture), emptyMap())
        coEvery { gameRepository.markReleaseNotificationDelivered(any(), any()) } answers {
            deliveredDate = secondArg()
        }
        coEvery { gameRepository.getReleaseNotificationDeliveredDate(any()) } answers { deliveredDate }
        // Holds the first sync inside its body until released.
        val gate = CompletableDeferred<Unit>()
        coEvery { scheduler.cancel(GAME_ID) } coAnswers { gate.await() }

        val first = launch { useCase() }
        advanceUntilIdle()
        val second = launch { useCase() }
        advanceUntilIdle()

        coVerify(exactly = 1) { scheduler.takeFiredReminders() }

        gate.complete(Unit)
        advanceUntilIdle()
        first.join()
        second.join()

        coVerify(exactly = 2) { scheduler.takeFiredReminders() }
        coVerify(exactly = 0) { scheduler.schedule(any()) }
    }
}
