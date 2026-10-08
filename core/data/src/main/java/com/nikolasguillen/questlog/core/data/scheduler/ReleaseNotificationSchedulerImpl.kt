package com.nikolasguillen.questlog.core.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nikolasguillen.questlog.core.data.worker.ReleaseNotificationWorker
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Instant

private const val GAME_ID_KEY = "gameId"
private fun releaseNotificationWorkName(gameId: Int) = "release_notification_$gameId"

class ReleaseNotificationSchedulerImpl(
    private val context: Context
) : ReleaseNotificationScheduler {
    // REPLACE, not KEEP: unlike Radar's refresh (ReleaseRefreshSchedulerImpl), a reschedule here is
    // meant to discard whatever was previously queued for this game -- that's what lets the reconciler
    // call schedule() repeatedly and stay idempotent instead of tracking whether a request already exists.
    override fun schedule(gameId: Int, at: Instant) {
        val initialDelay = (at - Clock.System.now()).coerceAtLeast(ZERO)
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

    override fun cancel(gameId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(releaseNotificationWorkName(gameId))
    }
}
