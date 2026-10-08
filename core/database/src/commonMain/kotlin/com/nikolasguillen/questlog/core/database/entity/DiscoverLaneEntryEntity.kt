package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import com.nikolasguillen.questlog.core.model.DiscoverLane

/**
 * One game's place in a cached lane's popularity order. [position] is explicit because `@Relation`
 * cannot sort and the order is user-visible — restored in [com.nikolasguillen.questlog.core.database.dao.DiscoverCacheDao].
 * `position = 0` on [DiscoverLane.MOST_ANTICIPATED] is the editorial hero pick: it is not stored
 * separately, it is simply the first entry.
 */
@Entity(
    tableName = "discover_lane_entries",
    primaryKeys = ["lane", "gameId"]
)
data class DiscoverLaneEntryEntity(
    val lane: DiscoverLane,
    val gameId: Int,
    val position: Int
)
