package com.nikolasguillen.questlog.core.database

import androidx.room.RoomDatabaseConstructor

/**
 * Room generates the `actual` for each target. [QuestLogDatabase] points at it with `@ConstructedBy`.
 */
@Suppress("KotlinNoActualForExpect")
expect object QuestLogDatabaseConstructor : RoomDatabaseConstructor<QuestLogDatabase> {
    override fun initialize(): QuestLogDatabase
}
