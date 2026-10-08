package com.nikolasguillen.questlog.core.domain.usecase.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.radar.resolveReleaseDates
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.DatePrecision
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The fire-time half of release notifications, called only by the notification worker. Re-applies the
 * full eligibility check right before posting — still opted in, still saved, still an exact date, release
 * day is today or earlier, not already notified for this date — which is what makes lazy reconciliation
 * ([SyncReleaseNotificationsUseCase]) safe: a stale schedule can decline to post here instead of ever
 * reaching the user.
 *
 * @return `true` if a notification was posted, `false` if this call correctly declined to.
 */
class DeliverReleaseNotificationUseCase(
    private val repository: GameRepository,
    private val notifier: ReleaseNotifier
) {
    suspend operator fun invoke(gameId: Int): Boolean {
        val optedIn = repository.getReleaseNotificationGameIds().first().contains(gameId)
        if (!optedIn) return false

        val game = repository.getSavedGames().first().find { it.id == gameId } ?: return false
        val ownedPlatformIds = repository.getOwnedPlatformIds().first()
        val earliestDate = game.resolveReleaseDates(ownedPlatformIds).minByOrNull { it.date ?: Long.MAX_VALUE }
            ?: return false
        if (earliestDate.precision != DatePrecision.EXACT_DATE) return false

        val releaseDateEpochSeconds = earliestDate.date ?: return false
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val releaseDay = Instant.fromEpochSeconds(releaseDateEpochSeconds).toLocalDateTime(timeZone).date
        if (releaseDay > today) return false

        if (repository.getReleaseNotificationDeliveredDate(gameId) == releaseDateEpochSeconds) return false

        notifier.notifyReleased(gameId, game.name)
        repository.markReleaseNotificationDelivered(gameId, releaseDateEpochSeconds)
        return true
    }
}
