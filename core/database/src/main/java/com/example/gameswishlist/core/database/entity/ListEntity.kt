package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String
)

@Entity(
    tableName = "game_list_cross_ref",
    primaryKeys = ["gameId", "listId"]
)
data class GameListCrossRef(
    val gameId: Int,
    val listId: Long
)
