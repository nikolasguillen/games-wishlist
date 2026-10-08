package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.radar.resolveNotificationInstant
import com.nikolasguillen.questlog.core.domain.radar.resolveReleaseDates
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.DatePrecision
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Reconciles every opted-in game's schedule against its currently resolved release date — the single
 * function that decides what is scheduled. Idempotent by construction: every enqueue is `REPLACE` on a
 * per-game unique work name, so calling this repeatedly for the same game changes nothing. Called from
 * [SetReleaseNotificationEnabledUseCase] (one game, right after a toggle) and from the periodic release
 * date refresh worker (every opt-in, after a successful refresh — this is what satisfies FR-007).
 */
class SyncReleaseNotificationsUseCase(
    private val repository: GameRepository,
    private val scheduler: ReleaseNotificationScheduler
) {
    suspend operator fun invoke(gameId: Int? = null) {
        val optedInIds = repository.getReleaseNotificationGameIds().first()
        // A specific gameId is always in scope, even if it was just disabled (the opt-in row is already
        // gone by the time SetReleaseNotificationEnabledUseCase calls this) — that case still needs its
        // schedule cancelled below, it just skips straight to it since there's nothing else to reconcile.
        val scope = if (gameId != null) setOf(gameId) else optedInIds
        if (scope.isEmpty()) return

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
            val instant = resolveNotificationInstant(
                releaseDateEpochSeconds = earliestDate?.date,
                precision = earliestDate?.precision ?: DatePrecision.TBD,
                now = Clock.System.now()
            )
            val notifiedForDate = repository.getReleaseNotificationDeliveredDate(id)

            if (instant == null || earliestDate?.date == notifiedForDate) {
                scheduler.cancel(id)
            } else {
                scheduler.schedule(id, instant)
            }
        }
    }
}
