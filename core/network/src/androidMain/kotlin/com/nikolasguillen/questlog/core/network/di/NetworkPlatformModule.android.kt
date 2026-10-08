package com.nikolasguillen.questlog.core.network.di

import android.os.SystemClock
import com.nikolasguillen.questlog.core.network.ElapsedRealtimeSource
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkPlatformModule: Module = module {
    // Time since boot, which keeps counting while the device sleeps: the token's expiry is measured on it.
    single { ElapsedRealtimeSource { SystemClock.elapsedRealtime() } }

    factory<HttpClientEngine> { OkHttp.create() }
}
