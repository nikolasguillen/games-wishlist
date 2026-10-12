package com.nikolasguillen.questlog.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nikolasguillen.questlog.core.common.applicationSupportDirectory
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.ArmedReminderLedger
import com.nikolasguillen.questlog.core.data.notification.LocalNotificationCenter
import com.nikolasguillen.questlog.core.data.notification.LocalNotificationReleaseScheduler
import com.nikolasguillen.questlog.core.data.notification.ReleaseReminderCoordinator
import com.nikolasguillen.questlog.core.data.notification.UserNotificationCenter
import com.nikolasguillen.questlog.core.data.scheduler.AppRefreshTaskHandler
import com.nikolasguillen.questlog.core.data.scheduler.BackgroundTaskReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshRunner
import com.nikolasguillen.questlog.core.data.translation.AppleLanguageModelTranslator
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import kotlinx.coroutines.flow.map
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
            syncReleaseNotifications = get(),
            settingsDataStore = get(),
            scope = get(),
            clock = Clock.System
        )
    }
    single { BackgroundTaskReleaseRefreshScheduler(runner = get(), bridge = get(), scope = get()) } binds
        arrayOf(ReleaseRefreshScheduler::class, AppRefreshTaskHandler::class)

    // Release reminders are local notifications that the system delivers by itself, so there is no worker and no
    // app code at fire time: the coordinator keeps the pending set right, and the ledger tells a reminder that
    // fired from one that was never armed. `ReminderSyncTriggers` is bound by `startQuestLog`, because the
    // permission state it listens to lives in :core:ui.
    single<LocalNotificationCenter> { UserNotificationCenter() }
    single { ArmedReminderLedger(get()) }
    single<ReleaseNotificationScheduler> {
        LocalNotificationReleaseScheduler(center = get(), text = get(), ledger = get(), clock = Clock.System)
    }
    single {
        val repository = get<GameRepository>()
        ReleaseReminderCoordinator(
            syncReleaseNotifications = get(),
            getSavedGameIds = { repository.getSavedGames().map { games -> games.map { it.id }.toSet() } },
            triggers = get(),
            scope = get()
        )
    }
    // No ReleaseNotifier: the system posts the notification, and the only caller of one, the Android worker, does
    // not exist here.

    // Needs the AppleLanguageModelBridge that startQuestLog binds from Swift: FoundationModels is Swift-only.
    singleOf(::AppleLanguageModelTranslator) { bind<GameDescriptionTranslator>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "${applicationSupportDirectory()}/datastore/settings.preferences_pb".toPath() }
        )
    }
}
