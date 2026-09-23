package com.example.gameswishlist.core.data.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gameswishlist.core.data.worker.ReleaseDatesRefreshWorker
import com.example.gameswishlist.core.domain.radar.ReleaseRefreshScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val RELEASE_DATES_REFRESH_WORK_NAME = "release_dates_refresh"
private const val IMMEDIATE_REFRESH_WORK_NAME = "release_dates_refresh_now"
private const val RELEASE_DATES_REFRESH_INTERVAL_HOURS = 24L

class ReleaseRefreshSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReleaseRefreshScheduler {
    // KEEP: called from Application.onCreate() on every launch, so an existing periodic schedule must
    // survive untouched rather than restart its 24h window each time the app is opened.
    override fun schedulePeriodicRefresh() {
        val request = PeriodicWorkRequestBuilder<ReleaseDatesRefreshWorker>(
            RELEASE_DATES_REFRESH_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RELEASE_DATES_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // KEEP: several saves in a row (e.g. from the search grid) each ask for a refresh; the first enqueue
    // wins and the rest collapse into it instead of piling up redundant runs.
    override fun scheduleImmediateRefresh() {
        val request = OneTimeWorkRequestBuilder<ReleaseDatesRefreshWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_REFRESH_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
