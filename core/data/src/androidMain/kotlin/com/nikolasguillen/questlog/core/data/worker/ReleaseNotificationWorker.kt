package com.nikolasguillen.questlog.core.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikolasguillen.questlog.core.domain.usecase.notification.DeliverReleaseNotificationUseCase

private const val GAME_ID_KEY = "gameId"

/**
 * One-shot job that fires a single opted-in game's release reminder. Always succeeds after a valid
 * [gameId]: [DeliverReleaseNotificationUseCase] re-verifies eligibility at fire time, so a stale schedule
 * declining to post is correct behavior, not a failure to retry.
 */
internal class ReleaseNotificationWorker(
    context: Context,
    params: WorkerParameters,
    private val deliverReleaseNotificationUseCase: DeliverReleaseNotificationUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val gameId = inputData.getInt(GAME_ID_KEY, -1)
        if (gameId == -1) return Result.failure()
        deliverReleaseNotificationUseCase(gameId)
        return Result.success()
    }
}
