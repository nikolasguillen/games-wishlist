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
     * Every platform the app has cached. The table is filled from two sides: saving a game writes the
     * platforms it runs on, and [insertPlatforms] writes IGDB's catalogue wholesale.
     */
    @Query("SELECT * FROM platforms ORDER BY name")
    fun getKnownPlatforms(): Flow<List<PlatformEntity>>

    /**
     * Writes a page of IGDB's platform catalogue. `REPLACE` refreshes rows a saved game had already
     * inserted; it cannot take their cross-refs down with them, because `game_platform_cross_ref`
     * declares no foreign key onto this table.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatforms(platforms: List<PlatformEntity>)

    @Query("SELECT platformId FROM owned_platforms")
    fun observeOwnedPlatformIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnedPlatform(owned: OwnedPlatformEntity)

    @Query("DELETE FROM owned_platforms")
    suspend fun clearOwnedPlatforms()

    /**
     * Replaces the whole selection. The rows are keyed by platform, not by a stable id of their own,
     * so a deselected platform has to be deleted rather than overwritten — a REPLACE insert alone
     * would leave it behind. An empty [platformIds] is a valid selection meaning "no platform filter",
     * not a missing one.
     */
    @Transaction
    suspend fun setOwnedPlatforms(platformIds: Set<Int>) {
        clearOwnedPlatforms()
        platformIds.forEach { insertOwnedPlatform(OwnedPlatformEntity(it)) }
    }
}
