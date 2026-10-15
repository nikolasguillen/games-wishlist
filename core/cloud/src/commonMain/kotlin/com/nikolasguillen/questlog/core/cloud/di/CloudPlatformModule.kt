package com.nikolasguillen.questlog.core.cloud.di

import org.koin.core.module.Module

/**
 * What each platform binds for the cloud:
 * - Android: `CurrentActivityTracker` and `PlatformProviderAuth` over Credential Manager and Firebase's Apple flow.
 * - iOS: `PlatformProviderAuth` over the Swift `IdentityProviderBridge`, which `startQuestLog` binds.
 */
expect val cloudPlatformModule: Module
