package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gameswishlist.core.model.GameStatus

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val ratingCount: Int = 0,
    val hypes: Int = 0,
    val metacritic: Int?,
    val gameTypeId: Int,
    val notes: String,
    val priority: Int?,
    val status: GameStatus?,
    val url: String?,
    val lastViewedAt: Long? = null
)
