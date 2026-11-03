package com.nikolasguillen.questlog.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotifications
import com.nikolasguillen.questlog.core.common.desktop.appDataDirectory
import com.nikolasguillen.questlog.core.common.desktop.desktopNotifications
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.ArmedReminderLedger
import com.nikolasguillen.questlog.core.data.notification.DesktopLocalNotificationCenter
import com.nikolasguillen.questlog.core.data.notification.LocalNotificationCenter
import com.nikolasguillen.questlog.core.data.notification.LocalNotificationReleaseScheduler
import com.nikolasguillen.questlog.core.data.notification.ReleaseReminderCoordinator
import com.nikolasguillen.questlog.core.data.notification.sweepOrphanReminders
import com.nikolasguillen.questlog.core.data.scheduler.InAppReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshRunner
import com.nikolasguillen.questlog.core.data.translation.UnsupportedDescriptionTranslator
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Clock

actual val dataPlatformModule: Module = module {
    // The refresh runs while the app is open: at launch, every hour after that, and when the saved set changes.
    // There is no operating-system job behind it, so nothing runs while the app is closed.
    single {
        ReleaseRefreshRunner(
            refreshReleaseDates = get(),
            syncReleaseNotifications = get(),
            settingsDataStore = get(),
            scope = get(),
            clock = Clock.System
        )
    }
    single<ReleaseRefreshScheduler> { InAppReleaseRefreshScheduler(runner = get(), scope = get()) }

    // Release reminders are scheduled notifications that the operating system delivers by itself, as on iOS: the
    // coordinator keeps the pending set right and the ledger tells a reminder that fired from one never armed.
    // `ReminderSyncTriggers` is bound by `startQuestLogDesktop`, because the permission state it listens to lives in
    // :core:ui. No ReleaseNotifier: no app code runs at fire time.
    single<DesktopNotifications> { desktopNotifications() }
    single<LocalNotificationCenter> { DesktopLocalNotificationCenter(notifications = get()) }
    single { ArmedReminderLedger(get()) }
    single<ReleaseNotificationScheduler> {
        LocalNotificationReleaseScheduler(center = get(), text = get(), ledger = get(), clock = Clock.System)
    }
    single {
        val repository = get<GameRepository>()
        val center = get<LocalNotificationCenter>()
        val ledger = get<ArmedReminderLedger>()
        ReleaseReminderCoordinator(
            syncReleaseNotifications = get(),
            getSavedGameIds = { repository.getSavedGames().map { games -> games.map { it.id }.toSet() } },
            triggers = get(),
            scope = get(),
            // Reminders a reinstall left in the system have no ledger entry: drop them before the launch sync.
            beforeFirstSync = { sweepOrphanReminders(center, ledger) }
        )
    }

    // Desktop has no on-device language model.
    singleOf(::UnsupportedDescriptionTranslator) { bind<GameDescriptionTranslator>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { appDataDirectory().resolve("datastore").resolve("settings.preferences_pb").toOkioPath() }
        )
    }
}
