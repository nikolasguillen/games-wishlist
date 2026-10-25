package com.nikolasguillen.questlog.core.network.di

import com.nikolasguillen.questlog.core.network.ElapsedRealtimeSource
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

actual val networkPlatformModule: Module = module {
    // The wall clock, not System.nanoTime(): the JVM's monotonic clock stops while a Mac sleeps, which would let a
    // token outlive its lifetime. The wall clock keeps counting through sleep, the way SystemClock.elapsedRealtime()
    // does on Android. A manual clock change only brings a refresh forward or puts it back.
    single { ElapsedRealtimeSource { System.currentTimeMillis() } }

    factory<HttpClientEngine> { OkHttp.create() }
}
