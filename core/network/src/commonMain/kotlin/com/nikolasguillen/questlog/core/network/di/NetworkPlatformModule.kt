package com.nikolasguillen.questlog.core.network.di

import org.koin.core.module.Module

/**
 * The bindings that differ per platform: the Ktor `HttpClientEngine` (a factory, one per client) and the
 * `ElapsedRealtimeSource` token expiry is measured on.
 */
expect val networkPlatformModule: Module
