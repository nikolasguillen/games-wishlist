package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "game_company_cross_ref",
    primaryKeys = ["gameId", "companyId"]
)
data class GameCompanyCrossRef(
    val gameId: Int,
    val companyId: Int,
    val isDeveloper: Boolean,
    val isPublisher: Boolean
)
