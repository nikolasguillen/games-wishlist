package com.nikolasguillen.questlog.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.nikolasguillen.questlog.core.common.applicationSupportDirectory
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.QuestLogDatabaseConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databasePlatformModule: Module = module {
    factory<RoomDatabase.Builder<QuestLogDatabase>> {
        Room.databaseBuilder<QuestLogDatabase>(
            name = "${applicationSupportDirectory()}/${QuestLogDatabase.DATABASE_NAME}",
            factory = { QuestLogDatabaseConstructor.initialize() }
        ).setQueryCoroutineContext(Dispatchers.IO)
    }
}
