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

    /**
     * Matches anywhere in the query, not just at the start: "origins" has to recall
     * "assassin's creed origins", which is the whole point of suggesting past searches. Rows that do
     * start with the typed text still come first — they are the ones the user is most likely typing
     * towards — and the rest fall back to most recent.
     */
    @Query(
        """
        SELECT * FROM search_history
        WHERE `query` LIKE '%' || :searchQuery || '%'
        ORDER BY (CASE WHEN `query` LIKE :searchQuery || '%' THEN 0 ELSE 1 END), timestamp DESC
        LIMIT 5
        """
    )
    suspend fun filterRecentSearches(searchQuery: String): List<SearchHistoryEntity>

    @Query("DELETE FROM search_history WHERE `query` = :searchQuery")
    suspend fun delete(searchQuery: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()
}