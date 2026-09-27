package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

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
    private val scheduler = mockk<ReleaseNotificationScheduler>(relaxed = true)

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
    }

    @Test
    fun `schedules an eligible future exact-date game`() = runTest {
        stub(optedIn = setOf(GAME_ID), saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))))

        useCase()

        coVerify(exactly = 1) { scheduler.schedule(GAME_ID, any()) }
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
        coVerify(exactly = 0) { scheduler.schedule(any(), any()) }
    }

    @Test
    fun `re-arms when the resolved date differs from the notified date`() = runTest {
        stub(
            optedIn = setOf(GAME_ID),
            saved = listOf(game(releaseDates = listOf(ReleaseDate(farFuture, PC, "PC")))),
            notifiedForDate = farFuture - 1_000
        )

        useCase()

        coVerify(exactly = 1) { scheduler.schedule(GAME_ID, any()) }
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
        coVerify(exactly = 0) { scheduler.schedule(any(), any()) }
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
        val instantSlot = slot<Instant>()
        coEvery { scheduler.schedule(GAME_ID, capture(instantSlot)) } just Runs

        useCase()

        val expected = resolveNotificationInstant(earliest, DatePrecision.EXACT_DATE, Clock.System.now())
        assertEquals(expected, instantSlot.captured)
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

        coVerify(exactly = 1) { scheduler.schedule(GAME_ID, any()) }
        coVerify(exactly = 0) { scheduler.schedule(OTHER_GAME_ID, any()) }
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
}
