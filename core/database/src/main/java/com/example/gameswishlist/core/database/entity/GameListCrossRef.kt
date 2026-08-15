package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "game_list_cross_ref",
    primaryKeys = ["gameId", "listId"]
)
data class GameListCrossRef(
    val gameId: Int,
    val listId: Long
)
