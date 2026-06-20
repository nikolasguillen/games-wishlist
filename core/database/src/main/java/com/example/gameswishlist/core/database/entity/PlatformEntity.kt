package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val abbreviation: String?
)

@Entity(
    tableName = "game_platform_cross_ref",
    primaryKeys = ["gameId", "platformId"]
)
data class GamePlatformCrossRef(
    val gameId: Int,
    val platformId: Int
)
