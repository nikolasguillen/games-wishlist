package com.nikolasguillen.questlog.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.nikolasguillen.questlog.core.common.desktop.appDataDirectory
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.QuestLogDatabaseConstructor
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databasePlatformModule: Module = module {
    // Next to the rest of the app's data: %LOCALAPPDATA%\QuestLog or ~/Library/Application Support/QuestLog.
    factory<RoomDatabase.Builder<QuestLogDatabase>> {
        Room.databaseBuilder<QuestLogDatabase>(
            name = appDataDirectory().resolve("questlog.db").toString(),
            factory = { QuestLogDatabaseConstructor.initialize() }
        ).setQueryCoroutineContext(Dispatchers.IO)
    }
}
