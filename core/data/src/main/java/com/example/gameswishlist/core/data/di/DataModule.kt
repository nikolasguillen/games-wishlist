package com.example.gameswishlist.core.data.di

import com.example.gameswishlist.core.data.repository.GameRepositoryImpl
import com.example.gameswishlist.core.data.translation.GameDescriptionTranslatorImpl
import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.domain.translation.GameDescriptionTranslator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

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

    companion object {
        // Backs GameDescriptionTranslatorImpl's model download: it must outlive any single
        // SettingsViewModel so leaving the Settings screen does not cancel a download in flight.
        @Provides
        @Singleton
        fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
