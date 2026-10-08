package com.nikolasguillen.questlog.core.network.di

import android.os.SystemClock
import com.nikolasguillen.questlog.core.network.BuildConfig
import com.nikolasguillen.questlog.core.network.ElapsedRealtimeSource
import com.nikolasguillen.questlog.core.network.IgdbApiService
import com.nikolasguillen.questlog.core.network.IgdbApiServiceImpl
import com.nikolasguillen.questlog.core.network.IgdbAuthManager
import com.nikolasguillen.questlog.core.network.IgdbAuthService
import com.nikolasguillen.questlog.core.network.IgdbAuthServiceImpl
import com.nikolasguillen.questlog.core.network.createAuthHttpClient
import com.nikolasguillen.questlog.core.network.createIgdbHttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

val networkModule = module {
    // Time since boot, which keeps counting while the device sleeps: the token's expiry is measured on it.
    single { ElapsedRealtimeSource { SystemClock.elapsedRealtime() } }

    // The auth call has a client of its own, so fetching a token never goes through the step that needs one.
    single<IgdbAuthService> { IgdbAuthServiceImpl(createAuthHttpClient(OkHttp.create())) }

    single { IgdbAuthManager(get(), get()) }

    single<IgdbApiService> {
        IgdbApiServiceImpl(
            createIgdbHttpClient(
                engine = OkHttp.create(),
                authManager = get(),
                clientId = BuildConfig.IGDB_CLIENT_ID,
                logBodies = BuildConfig.DEBUG
            )
        )
    }
}
