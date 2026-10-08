package com.nikolasguillen.questlog.core.common.di

import com.nikolasguillen.questlog.core.common.AppVersionProvider
import com.nikolasguillen.questlog.core.common.AppVersionProviderImpl
import com.nikolasguillen.questlog.core.common.NetworkStatusProvider
import com.nikolasguillen.questlog.core.common.NetworkStatusProviderImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val commonModule = module {
    singleOf(::AppVersionProviderImpl) { bind<AppVersionProvider>() }
    singleOf(::NetworkStatusProviderImpl) { bind<NetworkStatusProvider>() }
}
