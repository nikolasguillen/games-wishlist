package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity

/**
 * Carries the same developer/publisher split as [GameCompanyCrossRef]. Full snapshot fidelity is
 * required here, not cosmetic: saving a game straight off a Discover card persists the feed's whole
 * `Game` verbatim, and `developers` is what the taste profile's recommended shelves are built from.
 */
@Entity(
    tableName = "cached_game_company_cross_ref",
    primaryKeys = ["gameId", "companyId"]
)
data class CachedGameCompanyCrossRef(
    val gameId: Int,
    val companyId: Int,
    val isDeveloper: Boolean,
    val isPublisher: Boolean
)
