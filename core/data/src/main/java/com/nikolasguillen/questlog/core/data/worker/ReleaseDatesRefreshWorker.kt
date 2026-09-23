package com.nikolasguillen.questlog.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikolasguillen.questlog.core.domain.radar.RefreshReleaseDatesUseCase
import com.nikolasguillen.questlog.core.model.AppResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodic background job backing Radar: refreshes saved games' release dates against IGDB. */
@HiltWorker
class ReleaseDatesRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshReleaseDatesUseCase: RefreshReleaseDatesUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (refreshReleaseDatesUseCase()) {
        is AppResult.Success -> Result.success()
        is AppResult.Failure -> Result.retry()
    }
}
