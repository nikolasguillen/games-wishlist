package com.nikolasguillen.questlog

import android.app.Application
import androidx.work.Configuration
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.shared.di.initKoin
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory

class QuestLogApp : Application(), Configuration.Provider {

    private val releaseRefreshScheduler: ReleaseRefreshScheduler by inject()

    // WorkManager initializes on demand and reads this configuration, so its workers are built by Koin.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        initKoin(isDebugBuild = BuildConfig.DEBUG) {
            androidContext(this@QuestLogApp)
        }
        releaseRefreshScheduler.schedulePeriodicRefresh()
    }
}
