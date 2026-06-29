package com.example.gameswishlist.core.domain.repository

import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.WishlistList
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun searchGames(query: String): AppResult<List<Game>>
    suspend fun getSearchSuggestions(query: String): AppResult<List<Game>>
    suspend fun addSearchToHistory(query: String)
    fun getRecentSearchHistory(): Flow<List<String>>
    suspend fun getFilteredSearchHistory(query: String): List<String>
    suspend fun deleteSearchHistoryItem(query: String)
    suspend fun clearSearchHistory()
    suspend fun getGameDetail(id: Int): AppResult<Game>
    fun getRecentlyViewedGames(): Flow<List<Game>>
    suspend fun removeRecentGame(gameId: Int)
    suspend fun clearRecentGames()
    fun getWishlistedGames(): Flow<List<Game>>
    suspend fun toggleWishlist(game: Game)
    suspend fun updateGameDetails(game: Game)

    fun getAllLists(): Flow<List<WishlistList>>
    suspend fun createList(name: String, description: String)
    suspend fun addGameToList(gameId: Int, listId: Long)
    suspend fun removeGameFromList(gameId: Int, listId: Long)
    fun getGamesByList(listId: Long): Flow<List<Game>>
}