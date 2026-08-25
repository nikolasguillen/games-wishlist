package com.example.gameswishlist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gameswishlist.core.database.entity.OwnedPlatformEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {
    /**
     * Every platform the app has seen so far. The `platforms` table is only populated as a side
     * effect of saving a game, so this is not the full IGDB catalogue.
     */
    @Query("SELECT * FROM platforms ORDER BY name")
    fun getKnownPlatforms(): Flow<List<PlatformEntity>>

    /** Platforms carried by the games the user put in a list — the default when nothing is chosen. */
    @Query(
        "SELECT DISTINCT platforms.* FROM platforms " +
                "INNER JOIN game_platform_cross_ref ON platforms.id = game_platform_cross_ref.platformId " +
                "INNER JOIN game_list_cross_ref ON game_platform_cross_ref.gameId = game_list_cross_ref.gameId " +
                "ORDER BY platforms.name"
    )
    fun getInferredPlatforms(): Flow<List<PlatformEntity>>

    @Query("SELECT platformId FROM owned_platforms")
    fun observeOwnedPlatformIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnedPlatform(owned: OwnedPlatformEntity)

    @Query("DELETE FROM owned_platforms")
    suspend fun clearOwnedPlatforms()

    /**
     * Replaces the whole selection. The rows are keyed by platform, not by a stable id of their own,
     * so a deselected platform has to be deleted rather than overwritten — a REPLACE insert alone
     * would leave it behind. Passing an empty [platformIds] clears the override and falls back to
     * [getInferredPlatforms].
     */
    @Transaction
    suspend fun setOwnedPlatforms(platformIds: Set<Int>) {
        clearOwnedPlatforms()
        platformIds.forEach { insertOwnedPlatform(OwnedPlatformEntity(it)) }
    }
}
