package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.PlatformProviderAuth
import com.nikolasguillen.questlog.core.cloud.UnavailableCloudAuth
import com.nikolasguillen.questlog.core.cloud.UnavailableCloudBackupStore
import com.nikolasguillen.questlog.core.cloud.UnavailablePlatformProviderAuth
import org.koin.dsl.module

actual val cloudPlatformModule = module {
    // Temporary: the desktop cloud account is built later in specs/020-desktop-app (T096 replaces all three). This
    // module comes after `cloudModule` in the list, so the two stand-ins replace its Firebase bindings.
    single<PlatformProviderAuth> { UnavailablePlatformProviderAuth() }
    single<CloudAuth> { UnavailableCloudAuth() }
    single<CloudBackupStore> { UnavailableCloudBackupStore() }
}
