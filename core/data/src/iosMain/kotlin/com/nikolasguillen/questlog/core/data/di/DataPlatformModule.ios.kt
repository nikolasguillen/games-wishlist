package com.nikolasguillen.questlog.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nikolasguillen.questlog.core.common.applicationSupportDirectory
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.NoOpReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.data.notification.NoOpReleaseNotifier
import com.nikolasguillen.questlog.core.data.notification.StaticReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.data.scheduler.AppRefreshTaskHandler
import com.nikolasguillen.questlog.core.data.scheduler.BackgroundTaskReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshRunner
import com.nikolasguillen.questlog.core.data.translation.AppleLanguageModelTranslator
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.notification.ReleaseRemindersAvailability
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.time.Clock

actual val dataPlatformModule: Module = module {
    // The refresh runs in the app, at launch and when the saved set changes, and the system also launches it in
    // the background (BGAppRefreshTask). One runner behind both, so they never overlap. The scheduler is bound as
    // the domain contract and as the task's body, which `runAppRefreshTask` resolves for Swift. The
    // AppRefreshTaskBridge it submits through is bound by `startQuestLog`: only Swift can call BackgroundTasks.
    single {
        ReleaseRefreshRunner(
            refreshReleaseDates = get(),
            settingsDataStore = get(),
            scope = get(),
            clock = Clock.System
        )
    }
    single { BackgroundTaskReleaseRefreshScheduler(runner = get(), bridge = get(), scope = get()) } binds
        arrayOf(ReleaseRefreshScheduler::class, AppRefreshTaskHandler::class)

    // Release reminders are not on iOS yet, so every entry point to them is hidden and these are never reached;
    // they are bound so the shared use cases that take them still resolve.
    singleOf(::NoOpReleaseNotificationScheduler) { bind<ReleaseNotificationScheduler>() }
    singleOf(::NoOpReleaseNotifier) { bind<ReleaseNotifier>() }
    single<ReleaseRemindersAvailability> { StaticReleaseRemindersAvailability(isAvailable = false) }

    // Needs the AppleLanguageModelBridge that startQuestLog binds from Swift: FoundationModels is Swift-only.
    singleOf(::AppleLanguageModelTranslator) { bind<GameDescriptionTranslator>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "${applicationSupportDirectory()}/datastore/settings.preferences_pb".toPath() }
        )
    }
}
