package com.nikolasguillen.questlog.core.network.di

import com.nikolasguillen.questlog.core.network.ElapsedRealtimeSource
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.posix.CLOCK_MONOTONIC_RAW
import platform.posix.clock_gettime_nsec_np

private const val NANOS_PER_MILLI = 1_000_000L

@OptIn(ExperimentalForeignApi::class)
actual val networkPlatformModule: Module = module {
    // Time since boot that keeps counting while the device sleeps, like SystemClock.elapsedRealtime(): the
    // token's expiry is measured on it. CLOCK_MONOTONIC_RAW is mach_continuous_time; CLOCK_UPTIME_RAW would stop
    // in sleep and let a token outlive its lifetime.
    single {
        ElapsedRealtimeSource { clock_gettime_nsec_np(CLOCK_MONOTONIC_RAW.toUInt()).toLong() / NANOS_PER_MILLI }
    }

    factory<HttpClientEngine> { Darwin.create() }
}
