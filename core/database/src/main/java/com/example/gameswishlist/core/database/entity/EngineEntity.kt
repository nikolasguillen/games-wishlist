package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engines")
data class EngineEntity(
    @PrimaryKey val id: Int,
    val name: String
)
