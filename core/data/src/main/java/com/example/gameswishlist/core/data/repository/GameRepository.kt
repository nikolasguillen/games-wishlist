package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.WishlistList
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun searchGames(query: String): AppResult<List<Game>>
    suspend fun getGameDetail(id: Int): Game
    fun getWishlistedGames(): Flow<List<Game>>
    suspend fun toggleWishlist(game: Game)
    suspend fun updateGameDetails(game: Game)
    
    fun getAllLists(): Flow<List<WishlistList>>
    suspend fun createList(name: String, description: String)
    suspend fun addGameToList(gameId: Int, listId: Long)
    suspend fun removeGameFromList(gameId: Int, listId: Long)
    fun getGamesByList(listId: Long): Flow<List<Game>>
}
