package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.mapper.toCompanyEntities
import com.example.gameswishlist.core.data.mapper.toEntity
import com.example.gameswishlist.core.data.mapper.toGame
import com.example.gameswishlist.core.data.mapper.toGameCompanyCrossRefs
import com.example.gameswishlist.core.data.mapper.toGameGenreCrossRefs
import com.example.gameswishlist.core.data.mapper.toGamePlatformCrossRefs
import com.example.gameswishlist.core.data.mapper.toGenreEntities
import com.example.gameswishlist.core.data.mapper.toPlatformEntities
import com.example.gameswishlist.core.data.mapper.toRelatedGameEntities
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.WishlistConstants
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.network.IgdbApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
            val excludedIds = GameType.noisyTypes.joinToString(",") { it.id.toString() }
            val queryText = """
                search "$query";
                fields name, url, game_type, summary, first_release_date, cover.url, total_rating, total_rating_count, aggregated_rating, hypes, platforms.name, platforms.abbreviation, platforms.generation, platforms.category, platforms.platform_family, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher;
                where game_type != ($excludedIds) & version_parent = null;
                limit 500;
            """.trimIndent()
            val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.searchGames(body)
            AppResult.success(response.map { it.toGame() })
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override suspend fun getRemoteSearchSuggestions(query: String): AppResult<List<Game>> {
        return try {
            val queryText = """
                fields name, cover.url, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, first_release_date;
                where name ~ *"$query"* & version_parent = null & game_type = (0, 8, 9, 10, 11);
                sort hypes desc;
                limit 10;
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

    override fun getRecentSearchHistory(): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches().map { it.map { entity -> entity.query } }
    }

    override suspend fun getFilteredSearchHistory(query: String): List<String> {
        return searchHistoryDao.filterRecentSearches(query).map { entity -> entity.query }
    }

    override suspend fun deleteSearchHistoryItem(query: String) {
        searchHistoryDao.delete(query)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.deleteAll()
    }

    override suspend fun getGameDetail(id: Int): AppResult<Game> {
        return try {
            // Try local first
            val localGame = gameDao.getGameById(id)
            val isWishlisted = gameDao.isGameInList(id, WishlistConstants.DEFAULT_WISHLIST_ID)

            val game = if (localGame != null) {
                localGame.toGame().copy(isWishlisted = isWishlisted)
            } else {
                // Fetch from network
                val queryText = """
                    fields name, url, game_type, summary, first_release_date, cover.url, total_rating, aggregated_rating, platforms.name, platforms.abbreviation, platforms.generation, platforms.category, platforms.platform_family, release_dates.date, release_dates.platform.name, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, game_engines.name,
                    dlcs.name, dlcs.cover.url, expansions.name, expansions.cover.url, remakes.name, remakes.cover.url, remasters.name, remasters.cover.url, parent_game.name, parent_game.cover.url, artworks.url, screenshots.url;
                    where id = $id;
                """.trimIndent()
                val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
                val networkGame = apiService.getGameDetail(body).first()
                networkGame.toGame().copy(isWishlisted = isWishlisted)
            }

            // Update last viewed timestamp and save local
            val updatedGame = game.copy(lastViewedAt = System.currentTimeMillis())
            saveGameLocal(updatedGame)
            AppResult.success(updatedGame)
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override fun getRecentlyViewedGames(): Flow<List<Game>> {
        return combine(
            gameDao.getRecentlyViewedGames(),
            gameDao.getGameIdsInList(WishlistConstants.DEFAULT_WISHLIST_ID)
        ) { entities, wishlistIds ->
            entities.map { it.toGame().copy(isWishlisted = it.game.id in wishlistIds) }
        }
    }

    override suspend fun removeRecentGame(gameId: Int) {
        gameDao.clearLastViewedAt(gameId)
    }

    override suspend fun clearRecentGames() {
        gameDao.clearAllLastViewedAt()
    }

    override fun getWishlistedGames(): Flow<List<Game>> {
        return gameDao.getWishlistedGames().map { entities ->
            entities.map { it.toGame().copy(isWishlisted = true) }
        }
    }

    override suspend fun toggleWishlist(game: Game) {
        val isWishlisted = gameDao.isGameInList(game.id, WishlistConstants.DEFAULT_WISHLIST_ID)
        if (isWishlisted) {
            gameDao.deleteGameListCrossRef(
                GameListCrossRef(
                    game.id,
                    WishlistConstants.DEFAULT_WISHLIST_ID
                )
            )
        } else {
            // Ensure game is saved local before adding to list
            saveGameLocal(game)
            gameDao.insertGameListCrossRef(
                GameListCrossRef(
                    game.id,
                    WishlistConstants.DEFAULT_WISHLIST_ID
                )
            )
        }
    }

    override suspend fun updateGameDetails(game: Game) {
        saveGameLocal(game)
    }

    private suspend fun saveGameLocal(game: Game) {
        gameDao.saveGame(
            game = game.toEntity(),
            platforms = game.toPlatformEntities(),
            platformCrossRefs = game.toGamePlatformCrossRefs(),
            genres = game.toGenreEntities(),
            genreCrossRefs = game.toGameGenreCrossRefs(),
            companies = game.toCompanyEntities(),
            companyCrossRefs = game.toGameCompanyCrossRefs(),
            relatedGames = game.toRelatedGameEntities()
        )
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
        return combine(
            gameDao.getGamesByListId(listId),
            gameDao.getGameIdsInList(WishlistConstants.DEFAULT_WISHLIST_ID)
        ) { entities, wishlistIds ->
            entities.map { it.toGame().copy(isWishlisted = it.game.id in wishlistIds) }
        }
    }
}
