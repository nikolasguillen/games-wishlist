package com.nikolasguillen.questlog.core.database.di

import org.koin.core.module.Module

/**
 * Binds the `RoomDatabase.Builder<QuestLogDatabase>` that points at this platform's database file and runs its
 * queries on the platform's I/O dispatcher (`Dispatchers.IO` is not visible from `commonMain`). The common module
 * adds the driver, the seed and the rest.
 */
expect val databasePlatformModule: Module
