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
import com.nikolasguillen.questlog.core.data.scheduler.InProcessReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.data.translation.UnsupportedGameDescriptionTranslator
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
import org.koin.dsl.module
import kotlin.time.Clock

actual val dataPlatformModule: Module = module {
    // No background job runner on iOS: the refresh runs in the app, at launch and when the saved set changes.
    single<ReleaseRefreshScheduler> {
        InProcessReleaseRefreshScheduler(
            refreshReleaseDates = get(),
            settingsDataStore = get(),
            scope = get(),
            clock = Clock.System
        )
    }

    // Release reminders are not on iOS yet, so every entry point to them is hidden and these are never reached;
    // they are bound so the shared use cases that take them still resolve.
    singleOf(::NoOpReleaseNotificationScheduler) { bind<ReleaseNotificationScheduler>() }
    singleOf(::NoOpReleaseNotifier) { bind<ReleaseNotifier>() }
    single<ReleaseRemindersAvailability> { StaticReleaseRemindersAvailability(isAvailable = false) }

    singleOf(::UnsupportedGameDescriptionTranslator) { bind<GameDescriptionTranslator>() }
    factoryOf(::WishlistCoverImageStorageImpl) { bind<WishlistCoverImageStorage>() }

    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "${applicationSupportDirectory()}/datastore/settings.preferences_pb".toPath() }
        )
    }
}
