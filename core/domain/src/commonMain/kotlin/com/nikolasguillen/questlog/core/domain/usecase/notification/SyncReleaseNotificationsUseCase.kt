package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder
import com.nikolasguillen.questlog.core.domain.radar.resolveNotificationInstant
import com.nikolasguillen.questlog.core.domain.radar.resolveReleaseDates
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.DatePrecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * Reconciles every opted-in game's schedule against its currently resolved release date — the single
 * function that decides what is scheduled. Idempotent by construction: scheduling a game replaces whatever
 * was pending for it, so calling this repeatedly for the same game changes nothing. Called from
 * [SetReleaseNotificationEnabledUseCase] (one game, right after a toggle), from the periodic release date
 * refresh (every opt-in, after a successful refresh — this is what satisfies FR-007) and, on iOS, from the
 * reminder coordinator.
 *
 * It first records the reminders the system delivered without running app code
 * ([ReleaseNotificationScheduler.takeFiredReminders]), so that the loop below does not arm the same date again.
 *
 * The whole body runs under one lock. [ReleaseNotificationScheduler.takeFiredReminders] removes a fired
 * reminder from the scheduler's ledger before the delivered date is written, so a second sync running in
 * between would find neither and re-arm a reminder that already fired. The lock only serializes callers of this
 * instance, which is why it is bound as a `single`.
 */
class SyncReleaseNotificationsUseCase(
    private val repository: GameRepository,
    private val scheduler: ReleaseNotificationScheduler
) {
    private val mutex = Mutex()

    suspend operator fun invoke(gameId: Int? = null) = mutex.withLock {
        val optedInIds = repository.getReleaseNotificationGameIds().first()

        for ((id, releaseDate) in scheduler.takeFiredReminders()) {
            if (id in optedInIds) repository.markReleaseNotificationDelivered(id, releaseDate)
        }

        // A specific gameId is always in scope, even if it was just disabled (the opt-in row is already
        // gone by the time SetReleaseNotificationEnabledUseCase calls this) — that case still needs its
        // schedule cancelled below, it just skips straight to it since there's nothing else to reconcile.
        val scope = if (gameId != null) setOf(gameId) else optedInIds
        if (scope.isEmpty()) return@withLock

        val savedGames = repository.getSavedGames().first()
        val ownedPlatformIds = repository.getOwnedPlatformIds().first()

        for (id in scope) {
            if (id !in optedInIds) {
                scheduler.cancel(id)
                continue
            }
            val game = savedGames.find { it.id == id }
            if (game == null) {
                // The game left the saved set since it was opted in — prune it (FR-005's observable half).
                repository.setReleaseNotificationEnabled(id, false)
                scheduler.cancel(id)
                continue
            }

            val earliestDate = game.resolveReleaseDates(ownedPlatformIds).minByOrNull { it.date ?: Long.MAX_VALUE }
            val releaseDate = earliestDate?.date
            val instant = resolveNotificationInstant(
                releaseDateEpochSeconds = releaseDate,
                precision = earliestDate?.precision ?: DatePrecision.TBD,
                now = Clock.System.now()
            )
            val notifiedForDate = repository.getReleaseNotificationDeliveredDate(id)

            if (releaseDate == null || instant == null || releaseDate == notifiedForDate) {
                scheduler.cancel(id)
            } else {
                scheduler.schedule(ReleaseReminder(id, game.name, releaseDate, instant))
            }
        }
    }
}
