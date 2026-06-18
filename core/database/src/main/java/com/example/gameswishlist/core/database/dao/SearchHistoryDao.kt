package com.example.gameswishlist.core.database.dao

import androidx.room.Insert
import androidx.room.Query
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

interface SearchHistoryDao {
    @Insert
    suspend fun insert(searchHistory: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}