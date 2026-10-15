package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.IdentityProviderBridge
import com.nikolasguillen.questlog.core.cloud.IosProviderAuth
import com.nikolasguillen.questlog.core.cloud.PlatformProviderAuth
import org.koin.dsl.module

actual val cloudPlatformModule = module {
    // The bridge itself is bound by startQuestLog, like the other Swift bridges.
    single<PlatformProviderAuth> { IosProviderAuth(get<IdentityProviderBridge>()) }
}
