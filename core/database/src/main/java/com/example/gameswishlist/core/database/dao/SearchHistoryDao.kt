package com.example.gameswishlist.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE `query` LIKE :searchQuery || '%' ORDER BY timestamp DESC LIMIT 5")
    suspend fun filterRecentSearches(searchQuery: String): List<SearchHistoryEntity>

    @Query("DELETE FROM search_history WHERE `query` = :searchQuery")
    suspend fun delete(searchQuery: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}