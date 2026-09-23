package com.nikolasguillen.questlog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuestLogApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var releaseRefreshScheduler: ReleaseRefreshScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        releaseRefreshScheduler.schedulePeriodicRefresh()
    }
}
