package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.mapper.toEntity
import com.example.gameswishlist.core.data.mapper.toGame
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.network.IgdbApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val apiService: IgdbApiService,
    private val gameDao: GameDao,
    private val listDao: ListDao,
    private val searchHistoryDao: SearchHistoryDao
) : GameRepository {

    override suspend fun searchGames(query: String): AppResult<List<Game>> {
        return try {
            val queryText = """
                search "$query";
                fields name, summary, first_release_date, cover.url, total_rating, platforms.name, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher;
                limit 20;
            """.trimIndent()
            val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.searchGames(body)
            AppResult.success(response.map { it.toGame() })
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override suspend fun addSearchToHistory(query: String) {
        searchHistoryDao.insert(
            SearchHistoryEntity(
                query = query,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches().map { it.map { entity -> entity.query } }
    }

    override suspend fun deleteSearchHistoryItem(query: String) {
        searchHistoryDao.delete(query)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.deleteAll()
    }

    override suspend fun getGameDetail(id: Int): Game {
        // Try local first
        val localGame = gameDao.getGameById(id)
        if (localGame != null) return localGame.toGame()
        // Fetch from network
        return try {
            val queryText = """
                fields name, summary, first_release_date, cover.url, total_rating, platforms.name, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher;
                where id = $id;
            """.trimIndent()
            val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
            val networkGame = apiService.getGameDetail(body).first()
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
