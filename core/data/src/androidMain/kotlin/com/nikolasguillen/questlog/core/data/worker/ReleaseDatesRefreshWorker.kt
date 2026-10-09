package com.nikolasguillen.questlog.core.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase
import com.nikolasguillen.questlog.core.model.AppResult

/**
 * Periodic background job backing Radar: refreshes saved games' release dates against IGDB, then
 * reconciles every release-notification opt-in against the freshly refreshed dates. That second step is
 * what makes an opted-in game's reminder follow a shifted release date (FR-007) — it has to happen here
 * because this worker normally runs with the app's process dead.
 */
internal class ReleaseDatesRefreshWorker(
    context: Context,
    params: WorkerParameters,
    private val refreshReleaseDatesUseCase: RefreshReleaseDatesUseCase,
    private val syncReleaseNotificationsUseCase: SyncReleaseNotificationsUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (refreshReleaseDatesUseCase()) {
        is AppResult.Success -> {
            syncReleaseNotificationsUseCase()
            Result.success()
        }

        is AppResult.Failure -> Result.retry()
    }
}
