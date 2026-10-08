package com.nikolasguillen.questlog.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.nikolasguillen.questlog.core.ai.GeminiNanoClient
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.ReleaseNotifierImpl
import com.nikolasguillen.questlog.core.data.notification.StaticReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.data.repository.GameRepositoryImpl
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseNotificationSchedulerImpl
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshSchedulerImpl
import com.nikolasguillen.questlog.core.data.settings.AppearancePreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.settings.OnboardingPreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.settings.WishlistViewModePreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.translation.GameDescriptionTranslatorImpl
import com.nikolasguillen.questlog.core.data.worker.ReleaseDatesRefreshWorker
import com.nikolasguillen.questlog.core.data.worker.ReleaseNotificationWorker
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore
import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val dataModule = module {
    singleOf(::GameRepositoryImpl) { bind<GameRepository>() }
    singleOf(::GameDescriptionTranslatorImpl) { bind<GameDescriptionTranslator>() }
    singleOf(::ReleaseRefreshSchedulerImpl) { bind<ReleaseRefreshScheduler>() }
    singleOf(::ReleaseNotificationSchedulerImpl) { bind<ReleaseNotificationScheduler>() }
    singleOf(::ReleaseNotifierImpl) { bind<ReleaseNotifier>() }
    singleOf(::AppearancePreferenceStoreImpl) { bind<AppearancePreferenceStore>() }
    singleOf(::WishlistViewModePreferenceStoreImpl) { bind<WishlistViewModePreferenceStore>() }
    singleOf(::OnboardingPreferenceStoreImpl) { bind<OnboardingPreferenceStore>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    // Android has release reminders (WorkManager + notifications), so every reminder entry point shows.
    single<ReleaseRemindersAvailability> { StaticReleaseRemindersAvailability(isAvailable = true) }

    // The on-device model is shared by every app on the device and Generation.getClient() is a static
    // factory, so the one client is created lazily and kept.
    single { GeminiNanoClient() }

    // Backs GameDescriptionTranslatorImpl's model download: it must outlive any single
    // SettingsViewModel so leaving the Settings screen does not cancel a download in flight.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<DataStore<Preferences>> { androidContext().settingsDataStore }

    workerOf(::ReleaseDatesRefreshWorker)
    workerOf(::ReleaseNotificationWorker)
}
