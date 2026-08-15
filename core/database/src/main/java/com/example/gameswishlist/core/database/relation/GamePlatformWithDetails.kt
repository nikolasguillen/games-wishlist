package com.example.gameswishlist.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.PlatformEntity

data class GamePlatformWithDetails(
    @Embedded val crossRef: GamePlatformCrossRef,
    @Relation(
        parentColumn = "platformId",
        entityColumn = "id"
    )
    val platform: PlatformEntity
)
