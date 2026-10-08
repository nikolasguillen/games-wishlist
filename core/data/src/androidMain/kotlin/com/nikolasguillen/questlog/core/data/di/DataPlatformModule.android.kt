package com.nikolasguillen.questlog.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.nikolasguillen.questlog.core.ai.GeminiNanoClient
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.ReleaseNotifierImpl
import com.nikolasguillen.questlog.core.data.notification.StaticReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseNotificationSchedulerImpl
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshSchedulerImpl
import com.nikolasguillen.questlog.core.data.translation.GameDescriptionTranslatorImpl
import com.nikolasguillen.questlog.core.data.worker.ReleaseDatesRefreshWorker
import com.nikolasguillen.questlog.core.data.worker.ReleaseNotificationWorker
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val dataPlatformModule: Module = module {
    singleOf(::GameDescriptionTranslatorImpl) { bind<GameDescriptionTranslator>() }
    singleOf(::ReleaseRefreshSchedulerImpl) { bind<ReleaseRefreshScheduler>() }
    singleOf(::ReleaseNotificationSchedulerImpl) { bind<ReleaseNotificationScheduler>() }
    singleOf(::ReleaseNotifierImpl) { bind<ReleaseNotifier>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    // Android has release reminders (WorkManager + notifications), so every reminder entry point shows.
    single<ReleaseRemindersAvailability> { StaticReleaseRemindersAvailability(isAvailable = true) }

    // The on-device model is shared by every app on the device and Generation.getClient() is a static
    // factory, so the one client is created lazily and kept.
    single { GeminiNanoClient() }

    // The same file the `preferencesDataStore("settings")` delegate used, so an installed app keeps its settings.
    single<DataStore<Preferences>> {
        val context = androidContext()
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    }

    workerOf(::ReleaseDatesRefreshWorker)
    workerOf(::ReleaseNotificationWorker)
}
