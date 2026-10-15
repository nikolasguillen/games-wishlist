package com.nikolasguillen.questlog.core.data.di

import com.nikolasguillen.questlog.core.data.repository.AccountRepositoryImpl
import com.nikolasguillen.questlog.core.data.repository.BackupRepositoryImpl
import com.nikolasguillen.questlog.core.data.repository.GameRepositoryImpl
import com.nikolasguillen.questlog.core.data.settings.AppearancePreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.settings.OnboardingPreferenceStoreImpl
import com.nikolasguillen.questlog.core.data.settings.WishlistViewModePreferenceStoreImpl
import com.nikolasguillen.questlog.core.domain.repository.AccountRepository
import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore
import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * The bindings that need no platform API. Needs `dataPlatformModule` for the rest: the settings `DataStore`, the
 * schedulers, the notifier (Android only), the translator and the cover storage.
 */
val dataModule = module {
    singleOf(::GameRepositoryImpl) { bind<GameRepository>() }
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    singleOf(::BackupRepositoryImpl) { bind<BackupRepository>() }
    singleOf(::AppearancePreferenceStoreImpl) { bind<AppearancePreferenceStore>() }
    singleOf(::WishlistViewModePreferenceStoreImpl) { bind<WishlistViewModePreferenceStore>() }
    singleOf(::OnboardingPreferenceStoreImpl) { bind<OnboardingPreferenceStore>() }

    // A newer build may add keys to the backup, and an older build must still read the rest (research R7).
    single { Json { ignoreUnknownKeys = true; explicitNulls = false } }

    single<Clock> { Clock.System }

    // Outlives any single screen, so work started for one - a model download - is not cancelled when the user
    // leaves it.
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
