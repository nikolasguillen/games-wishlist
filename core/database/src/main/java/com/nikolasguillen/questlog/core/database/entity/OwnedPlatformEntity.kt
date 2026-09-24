package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A platform the user declared they own in Settings' "My platforms" picker.
 *
 * One row per platform rather than a single multi-value column, so the table holds exactly what the
 * user chose and nothing is inferred on their behalf: an empty table means no platform filter at all.
 */
@Entity(tableName = "owned_platforms")
data class OwnedPlatformEntity(
    @PrimaryKey val platformId: Int
)
