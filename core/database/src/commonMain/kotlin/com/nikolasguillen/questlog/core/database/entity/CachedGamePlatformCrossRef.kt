package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity

/**
 * No `releaseDate`/`releaseDatePrecision` columns, unlike [GamePlatformCrossRef]: the lane hydrate query
 * never requests `release_dates`, so there is nothing to store.
 */
@Entity(
    tableName = "cached_game_platform_cross_ref",
    primaryKeys = ["gameId", "platformId"]
)
data class CachedGamePlatformCrossRef(
    val gameId: Int,
    val platformId: Int
)
