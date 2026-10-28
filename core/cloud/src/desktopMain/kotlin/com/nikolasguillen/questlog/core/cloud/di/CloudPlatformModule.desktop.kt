package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.PlatformProviderAuth
import com.nikolasguillen.questlog.core.cloud.UnavailablePlatformProviderAuth
import org.koin.dsl.module

actual val cloudPlatformModule = module {
    // Temporary: the desktop cloud account is built later in specs/020-desktop-app (T096 replaces this).
    single<PlatformProviderAuth> { UnavailablePlatformProviderAuth() }
}
