package com.nikolasguillen.questlog.core.common.di

import com.nikolasguillen.questlog.core.common.AppVersionProvider
import com.nikolasguillen.questlog.core.common.AppVersionProviderImpl
import com.nikolasguillen.questlog.core.common.NetworkStatusProvider
import com.nikolasguillen.questlog.core.common.NetworkStatusProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(impl: AppVersionProviderImpl): AppVersionProvider

    @Binds
    @Singleton
    abstract fun bindNetworkStatusProvider(impl: NetworkStatusProviderImpl): NetworkStatusProvider
}
