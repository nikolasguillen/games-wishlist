package com.example.gameswishlist.core.database.dao

import androidx.room.*
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE isWishlisted = 1")
    fun getWishlistedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("SELECT * FROM games INNER JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId WHERE game_list_cross_ref.listId = :listId")
    fun getGamesByListId(listId: Long): Flow<List<GameEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameListCrossRef(crossRef: GameListCrossRef)

    @Delete
    suspend fun deleteGameListCrossRef(crossRef: GameListCrossRef)
}
