package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.FirebaseCloudAuth
import com.nikolasguillen.questlog.core.cloud.FirestoreBackupStore
import org.koin.dsl.module

/** The Firebase implementations, identical on both platforms. The provider sign-in is in [cloudPlatformModule]. */
val cloudModule = module {
    single<CloudAuth> { FirebaseCloudAuth(platform = get()) }
    single<CloudBackupStore> { FirestoreBackupStore() }
}
