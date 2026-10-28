package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.UnavailableCloudAuth
import com.nikolasguillen.questlog.core.cloud.UnavailableCloudBackupStore
import org.koin.dsl.module

actual val cloudPlatformModule = module {
    // Temporary: the desktop REST client replaces both in specs/020-desktop-app (T096).
    single<CloudAuth> { UnavailableCloudAuth() }
    single<CloudBackupStore> { UnavailableCloudBackupStore() }
}
