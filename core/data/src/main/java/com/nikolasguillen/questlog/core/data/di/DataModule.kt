package com.nikolasguillen.questlog.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.nikolasguillen.questlog.core.data.notification.ReleaseNotifierImpl
import com.nikolasguillen.questlog.core.data.repository.GameRepositoryImpl
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseNotificationSchedulerImpl
import com.nikolasguillen.questlog.core.data.scheduler.ReleaseRefreshSchedulerImpl
import com.nikolasguillen.questlog.core.data.settings.AppearancePreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.settings.WishlistViewModePreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.translation.GameDescriptionTranslatorImpl
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotificationScheduler
import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(
        gameRepositoryImpl: GameRepositoryImpl
    ): GameRepository

    @Binds
    @Singleton
    abstract fun bindGameDescriptionTranslator(
        gameDescriptionTranslatorImpl: GameDescriptionTranslatorImpl
    ): GameDescriptionTranslator

    @Binds
    @Singleton
    abstract fun bindReleaseRefreshScheduler(
        releaseRefreshSchedulerImpl: ReleaseRefreshSchedulerImpl
    ): ReleaseRefreshScheduler

    @Binds
    @Singleton
    abstract fun bindReleaseNotificationScheduler(
        releaseNotificationSchedulerImpl: ReleaseNotificationSchedulerImpl
    ): ReleaseNotificationScheduler

    @Binds
    @Singleton
    abstract fun bindReleaseNotifier(
        releaseNotifierImpl: ReleaseNotifierImpl
    ): ReleaseNotifier

    @Binds
    @Singleton
    abstract fun bindAppearancePreferenceStore(
        appearancePreferenceStoreImpl: AppearancePreferenceStoreImpl
    ): AppearancePreferenceStore

    @Binds
    @Singleton
    abstract fun bindWishlistViewModePreferenceStore(
        wishlistViewModePreferenceStoreImpl: WishlistViewModePreferenceStoreImpl
    ): WishlistViewModePreferenceStore

    companion object {
        // Backs GameDescriptionTranslatorImpl's model download: it must outlive any single
        // SettingsViewModel so leaving the Settings screen does not cancel a download in flight.
        @Provides
        @Singleton
        fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.settingsDataStore
    }
}
