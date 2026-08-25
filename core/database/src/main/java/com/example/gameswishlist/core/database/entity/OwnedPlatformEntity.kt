package com.example.gameswishlist.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A platform the user declared they own, as an explicit override of the set inferred from their
 * saved games.
 *
 * One row per platform rather than a single multi-value column: an empty table is the "not set"
 * state, which is what makes the inferred default distinguishable from a deliberate empty choice.
 */
@Entity(tableName = "owned_platforms")
data class OwnedPlatformEntity(
    @PrimaryKey val platformId: Int
)
