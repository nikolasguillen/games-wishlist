package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: Int,
    val name: String
)

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
