package com.nikolasguillen.questlog.core.cloud.di

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.FirebaseCloudAuth
import com.nikolasguillen.questlog.core.cloud.FirestoreBackupStore
import org.koin.dsl.module

/**
 * The Firebase implementations, identical on Android and iOS; each platform's `cloudPlatformModule` includes it and
 * supplies the provider sign-in. Desktop has no Firebase SDK and binds its own REST implementations.
 */
internal val firebaseCloudModule = module {
    single<CloudAuth> { FirebaseCloudAuth(platform = get()) }
    single<CloudBackupStore> { FirestoreBackupStore() }
}
