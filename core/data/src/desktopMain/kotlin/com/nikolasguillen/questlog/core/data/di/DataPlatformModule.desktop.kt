package com.nikolasguillen.questlog.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nikolasguillen.questlog.core.common.desktop.appDataDirectory
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorageImpl
import com.nikolasguillen.questlog.core.data.notification.InertReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.data.scheduler.InAppReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshRunner
import com.nikolasguillen.questlog.core.data.translation.UnsupportedDescriptionTranslator
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
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

    // Placeholder until the reminders are built (specs/020-desktop-app, T062 replaces it).
    single<ReleaseNotificationScheduler> { InertReleaseNotificationScheduler() }
    // No ReleaseNotifier: the operating system posts the notification, as on iOS.

    // Desktop has no on-device language model.
    singleOf(::UnsupportedDescriptionTranslator) { bind<GameDescriptionTranslator>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { appDataDirectory().resolve("datastore").resolve("settings.preferences_pb").toOkioPath() }
        )
    }
}
