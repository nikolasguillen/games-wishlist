package com.nikolasguillen.questlog.core.common.di

import org.koin.core.module.Module

/**
 * The bindings that need a platform API: the installed app version and the network status. Each platform
 * supplies the module that binds its own implementations.
 */
expect val commonPlatformModule: Module
