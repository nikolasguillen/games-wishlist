package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nikolasguillen.questlog.core.model.DiscoverLane

/**
 * The freshness stamp for one generic Discover lane. A row exists only after a successful fetch — an
 * empty-but-successful fetch still writes one, so an empty lane is not re-fetched on every open.
 */
@Entity(tableName = "discover_lane_cache")
data class DiscoverLaneCacheEntity(
    @PrimaryKey val lane: DiscoverLane,
    val fetchedAt: Long
)
