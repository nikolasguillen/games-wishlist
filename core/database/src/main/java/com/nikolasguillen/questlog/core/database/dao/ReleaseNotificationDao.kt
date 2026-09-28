package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikolasguillen.questlog.core.database.entity.ReleaseNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReleaseNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReleaseNotificationEntity)

    @Query("DELETE FROM release_notifications WHERE gameId = :gameId")
    suspend fun delete(gameId: Int)

    @Query("SELECT gameId FROM release_notifications")
    fun observeGameIds(): Flow<List<Int>>

    @Query("SELECT * FROM release_notifications WHERE gameId = :gameId")
    suspend fun get(gameId: Int): ReleaseNotificationEntity?

    @Query("UPDATE release_notifications SET notifiedForDate = :date WHERE gameId = :gameId")
    suspend fun markNotified(gameId: Int, date: Long)
}
