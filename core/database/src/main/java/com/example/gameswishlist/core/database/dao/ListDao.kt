package com.example.gameswishlist.core.database.dao

import androidx.room.*
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.relation.ListWithGameCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query(
        """
        SELECT wishlists.*, COUNT(game_list_cross_ref.gameId) AS gameCount
        FROM wishlists
        LEFT JOIN game_list_cross_ref ON wishlists.id = game_list_cross_ref.listId
        GROUP BY wishlists.id
        """
    )
    fun getAllLists(): Flow<List<ListWithGameCount>>

    @Query("SELECT * FROM wishlists WHERE id = :listId")
    suspend fun getListById(listId: Long): ListEntity?

    @Query("SELECT * FROM wishlists WHERE id = :listId")
    fun observeListById(listId: Long): Flow<ListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity)

    @Update
    suspend fun updateList(list: ListEntity)

    @Delete
    suspend fun deleteList(list: ListEntity)

    @Query("DELETE FROM game_list_cross_ref WHERE listId = :listId")
    suspend fun deleteGameRefsForList(listId: Long)

    /**
     * The cross-ref table has no foreign key on the list, so its rows have to be removed
     * explicitly or they would outlive the list they point at.
     */
    @Transaction
    suspend fun deleteListWithGameRefs(list: ListEntity) {
        deleteGameRefsForList(list.id)
        deleteList(list)
    }
}
