package com.nikolasguillen.questlog.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databasePlatformModule: Module = module {
    // The same file as before the conversion: databases/quest_log_database.
    factory<RoomDatabase.Builder<QuestLogDatabase>> {
        Room.databaseBuilder(androidContext(), QuestLogDatabase::class.java, QuestLogDatabase.DATABASE_NAME)
            .setQueryCoroutineContext(Dispatchers.IO)
    }
}
