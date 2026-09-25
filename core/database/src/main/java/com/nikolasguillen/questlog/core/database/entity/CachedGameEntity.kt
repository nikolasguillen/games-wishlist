package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A catalogue snapshot for a game shown in a generic Discover lane. Deliberately carries none of
 * [GameEntity]'s user-owned columns (`notes`, `priority`, `status`, `lastViewedAt`, `detailsFetchedAt`)
 * — that omission is what keeps a lane cache from ever being mistaken for the user's own games.
 */
@Entity(tableName = "cached_games")
data class CachedGameEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val ratingCount: Int,
    val hypes: Int,
    val metacritic: Int?,
    val gameTypeId: Int,
    val url: String?
)
