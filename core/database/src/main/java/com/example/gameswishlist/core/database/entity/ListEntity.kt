package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.example.gameswishlist.core.model.WishlistIcon

@Entity(tableName = "wishlists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val icon: WishlistIcon? = null
)

@Entity(
    tableName = "game_list_cross_ref",
    primaryKeys = ["gameId", "listId"]
)
data class GameListCrossRef(
    val gameId: Int,
    val listId: Long
)
