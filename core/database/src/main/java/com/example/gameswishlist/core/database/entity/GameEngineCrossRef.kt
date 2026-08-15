package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "game_engine_cross_ref",
    primaryKeys = ["gameId", "engineId"]
)
data class GameEngineCrossRef(
    val gameId: Int,
    val engineId: Int
)
