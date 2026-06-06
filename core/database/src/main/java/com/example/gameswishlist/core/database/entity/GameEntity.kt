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
    val metacritic: Int?,
    val platforms: String, // Comma separated
    val genres: String, // Comma separated
    val publishers: String, // Comma separated
    val developers: String, // Comma separated
    val isWishlisted: Boolean,
    val notes: String,
    val priority: Int,
    val status: GameStatus
)
