package com.nikolasguillen.questlog

import android.app.Application
import androidx.work.Configuration
import com.nikolasguillen.questlog.core.common.di.commonPlatformModule
import com.nikolasguillen.questlog.core.data.di.dataModule
import com.nikolasguillen.questlog.core.database.di.databaseModule
import com.nikolasguillen.questlog.core.domain.di.domainModule
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.network.di.networkModule
import com.nikolasguillen.questlog.core.network.di.networkPlatformModule
import com.nikolasguillen.questlog.di.interimModule
import com.nikolasguillen.questlog.di.viewModelModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.context.startKoin

class QuestLogApp : Application(), Configuration.Provider {

    private val releaseRefreshScheduler: ReleaseRefreshScheduler by inject()

    // WorkManager initializes on demand and reads this configuration, so its workers are built by Koin.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(KoinWorkerFactory())
            .build()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QuestLogApp)
            modules(
                commonPlatformModule, domainModule, networkModule, networkPlatformModule, databaseModule, dataModule,
                interimModule, viewModelModule
            )
        }
        releaseRefreshScheduler.schedulePeriodicRefresh()
    }
}
