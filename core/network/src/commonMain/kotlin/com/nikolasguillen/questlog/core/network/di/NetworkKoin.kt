package com.nikolasguillen.questlog.core.network.di

import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.IgdbApiServiceImpl
import com.nikolasguillen.questlog.core.network.IgdbAuthManager
import com.nikolasguillen.questlog.core.network.IgdbAuthService
import com.nikolasguillen.questlog.core.network.IgdbAuthServiceImpl
import com.nikolasguillen.questlog.core.network.IgdbCredentials
import com.nikolasguillen.questlog.core.network.NetworkConfig
import com.nikolasguillen.questlog.core.network.createAuthHttpClient
import com.nikolasguillen.questlog.core.network.createIgdbHttpClient
import org.koin.dsl.module

/**
 * Needs a `NetworkConfig` from the app, and `networkPlatformModule` for the HTTP engine and the clock.
 */
val networkModule = module {
    // The auth call has a client of its own, so fetching a token never goes through the step that needs one.
    // The engine is a factory binding: each client owns its engine, as it did when each created its own.
    single<IgdbAuthService> { IgdbAuthServiceImpl(createAuthHttpClient(get())) }

    single { IgdbAuthManager(get(), get()) }

    single<IgdbApiService> {
        IgdbApiServiceImpl(
            createIgdbHttpClient(
                engine = get(),
                authManager = get(),
                clientId = IgdbCredentials.IGDB_CLIENT_ID,
                logBodies = get<NetworkConfig>().logBodies
            )
        )
    }
}
