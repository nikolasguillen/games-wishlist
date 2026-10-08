package com.nikolasguillen.questlog.shared.di

import com.nikolasguillen.questlog.core.common.di.commonPlatformModule
import com.nikolasguillen.questlog.core.data.di.dataModule
import com.nikolasguillen.questlog.core.data.di.dataPlatformModule
import com.nikolasguillen.questlog.core.database.DefaultWishlistSeed
import com.nikolasguillen.questlog.core.database.DefaultWishlistSeedProvider
import com.nikolasguillen.questlog.core.database.di.databaseModule
import com.nikolasguillen.questlog.core.database.di.databasePlatformModule
import com.nikolasguillen.questlog.core.domain.di.domainModule
import com.nikolasguillen.questlog.core.network.NetworkConfig
import com.nikolasguillen.questlog.core.network.di.networkModule
import com.nikolasguillen.questlog.core.network.di.networkPlatformModule
import com.nikolasguillen.questlog.shared.runBlockingCompat
import com.nikolasguillen.questlog.shared.resources.Res
import com.nikolasguillen.questlog.shared.resources.default_wishlist_description
import com.nikolasguillen.questlog.shared.resources.default_wishlist_name
import org.jetbrains.compose.resources.getString
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Starts Koin with every layer's modules. The platform entry point supplies what only it knows through
 * [appDeclaration] - on Android the application context and the WorkManager factory.
 *
 * @param isDebugBuild Whether request and response bodies are logged.
 */
fun initKoin(isDebugBuild: Boolean, appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(allModules(isDebugBuild))
}

/**
 * Every module of the app, in one list so that the graph test checks exactly what the app starts.
 */
internal fun allModules(isDebugBuild: Boolean): List<Module> = listOf(
    commonPlatformModule,
    domainModule,
    networkModule,
    networkPlatformModule,
    databaseModule,
    databasePlatformModule,
    dataModule,
    dataPlatformModule,
    viewModelModule,
    sharedModule(isDebugBuild)
)

/**
 * The bindings only the app root can supply: what is a debug build, and the words of the default wishlist.
 */
internal fun sharedModule(isDebugBuild: Boolean): Module = module {
    single { NetworkConfig(logBodies = isDebugBuild) }

    // Resolved when the database is first created, which happens on its own thread and once per install.
    single {
        DefaultWishlistSeedProvider {
            runBlockingCompat {
                DefaultWishlistSeed(
                    name = getString(Res.string.default_wishlist_name),
                    description = getString(Res.string.default_wishlist_description)
                )
            }
        }
    }
}
