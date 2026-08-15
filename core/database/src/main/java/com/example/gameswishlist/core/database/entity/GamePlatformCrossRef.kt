package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "game_platform_cross_ref",
    primaryKeys = ["gameId", "platformId"]
)
data class GamePlatformCrossRef(
    val gameId: Int,
    val platformId: Int,
    val releaseDate: Long? = null
)
