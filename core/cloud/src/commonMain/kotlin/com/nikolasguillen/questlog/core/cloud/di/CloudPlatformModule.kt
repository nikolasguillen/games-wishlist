package com.nikolasguillen.questlog.core.cloud.di

import org.koin.core.module.Module

/**
 * What each platform binds for the cloud:
 * - Android: `CurrentActivityTracker` and `PlatformProviderAuth` over Credential Manager and Firebase's Apple flow.
 * - iOS: `PlatformProviderAuth` over the Swift `IdentityProviderBridge`, which `startQuestLog` binds.
 * - Desktop: `CloudAuth` and `CloudBackupStore` over REST. Android and iOS get theirs from `firebaseCloudModule`.
 */
expect val cloudPlatformModule: Module
