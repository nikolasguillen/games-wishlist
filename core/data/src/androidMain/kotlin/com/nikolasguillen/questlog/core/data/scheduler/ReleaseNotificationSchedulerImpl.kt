package com.nikolasguillen.questlog.core.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikolasguillen.questlog.core.data.worker.ReleaseNotificationWorker
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseReminder
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.ZERO

private const val GAME_ID_KEY = "gameId"
private fun releaseNotificationWorkName(gameId: Int) = "release_notification_$gameId"

internal class ReleaseNotificationSchedulerImpl(
    private val context: Context
) : ReleaseNotificationScheduler {
    // REPLACE, not KEEP: unlike Radar's refresh (ReleaseRefreshSchedulerImpl), a reschedule here is
    // meant to discard whatever was previously queued for this game -- that's what lets the reconciler
    // call schedule() repeatedly and stay idempotent instead of tracking whether a request already exists.
    //
    // The reminder's name and release date are for platforms whose reminders run no app code at fire time; the
    // worker re-reads the game itself.
    override suspend fun schedule(reminder: ReleaseReminder) {
        val gameId = reminder.gameId
        val initialDelay = (reminder.fireAt - Clock.System.now()).coerceAtLeast(ZERO)
        val request = OneTimeWorkRequestBuilder<ReleaseNotificationWorker>()
            .setInitialDelay(initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(GAME_ID_KEY to gameId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            releaseNotificationWorkName(gameId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override suspend fun cancel(gameId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(releaseNotificationWorkName(gameId))
    }

    // The worker records delivery itself, so there is nothing to report.
    override suspend fun takeFiredReminders(): Map<Int, Long> = emptyMap()
}
