package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "game_genre_cross_ref",
    primaryKeys = ["gameId", "genreId"]
)
data class GameGenreCrossRef(
    val gameId: Int,
    val genreId: Int
)
