package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "related_games",
    primaryKeys = ["parentId", "relatedGameId", "relationType"]
)
data class RelatedGameEntity(
    val parentId: Int,
    val relatedGameId: Int,
    val name: String,
    val coverUrl: String?,
    val relationType: String
)
