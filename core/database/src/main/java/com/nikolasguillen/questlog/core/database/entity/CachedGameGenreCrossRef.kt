package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "cached_game_genre_cross_ref",
    primaryKeys = ["gameId", "genreId"]
)
data class CachedGameGenreCrossRef(
    val gameId: Int,
    val genreId: Int
)
