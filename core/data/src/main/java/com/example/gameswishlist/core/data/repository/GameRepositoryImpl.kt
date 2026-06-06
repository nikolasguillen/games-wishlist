package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.mapper.toEntity
import com.example.gameswishlist.core.data.mapper.toGame
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.network.RawgApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class GameRepositoryImpl @Inject constructor(
    private val apiService: RawgApiService,
    private val gameDao: GameDao,
    private val listDao: ListDao,
    @Named("RAWG_API_KEY") private val apiKey: String
) : GameRepository {

    override suspend fun searchGames(query: String): List<Game> {
        return try {
            val response = apiService.searchGames(apiKey, query)
            response.results.map { it.toGame() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGameDetail(id: Int): Game {
        // Try local first
        val localGame = gameDao.getGameById(id)
        if (localGame != null) return localGame.toGame()
        
        // Fetch from network
        return try {
            val networkGame = apiService.getGameDetail(id, apiKey)
            networkGame.toGame()
        } catch (e: Exception) {
            throw e
        }
    }

    override fun getWishlistedGames(): Flow<List<Game>> {
        return gameDao.getWishlistedGames().map { entities ->
            entities.map { it.toGame() }
        }
    }

    override suspend fun toggleWishlist(game: Game) {
        val updatedGame = game.copy(isWishlisted = !game.isWishlisted)
        gameDao.insertGame(updatedGame.toEntity())
    }

    override suspend fun updateGameDetails(game: Game) {
        gameDao.insertGame(game.toEntity())
    }

    override fun getAllLists(): Flow<List<WishlistList>> {
        return listDao.getAllLists().map { entities ->
            entities.map { WishlistList(it.id, it.name, it.description) }
        }
    }

    override suspend fun createList(name: String, description: String) {
        listDao.insertList(ListEntity(name = name, description = description))
    }

    override suspend fun addGameToList(gameId: Int, listId: Long) {
        gameDao.insertGameListCrossRef(GameListCrossRef(gameId, listId))
    }

    override suspend fun removeGameFromList(gameId: Int, listId: Long) {
        gameDao.deleteGameListCrossRef(GameListCrossRef(gameId, listId))
    }

    override fun getGamesByList(listId: Long): Flow<List<Game>> {
        return gameDao.getGamesByListId(listId).map { entities ->
            entities.map { it.toGame() }
        }
    }
}
