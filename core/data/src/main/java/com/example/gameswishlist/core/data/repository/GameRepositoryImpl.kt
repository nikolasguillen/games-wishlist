package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.data.mapper.toArtworkEntities
import com.example.gameswishlist.core.data.mapper.toCompanyEntities
import com.example.gameswishlist.core.data.mapper.toEngineEntities
import com.example.gameswishlist.core.data.mapper.toEntity
import com.example.gameswishlist.core.data.mapper.toGame
import com.example.gameswishlist.core.data.mapper.toGameCompanyCrossRefs
import com.example.gameswishlist.core.data.mapper.toGameEngineCrossRefs
import com.example.gameswishlist.core.data.mapper.toGameGenreCrossRefs
import com.example.gameswishlist.core.data.mapper.toGamePlatformCrossRefs
import com.example.gameswishlist.core.data.mapper.toGenreEntities
import com.example.gameswishlist.core.data.mapper.toPlatform
import com.example.gameswishlist.core.data.mapper.toPlatformEntities
import com.example.gameswishlist.core.data.mapper.toRelatedGameEntities
import com.example.gameswishlist.core.data.mapper.toWishlistList
import com.example.gameswishlist.core.data.local.WishlistCoverImageStorage
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.PlatformDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.model.WishlistConstants
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.network.IgdbApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * The autocomplete dropdown shows at most four games — with the keyboard open there is no room for
 * more once the recent queries and the "see all results" row are counted, so asking IGDB for ten
 * only spends rate budget on rows nobody sees.
 */
private const val SUGGESTIONS_LIMIT = 4

/**
 * IGDB Popularity API type ids used by the two Discover lanes (IGDB source, `external_popularity_source`
 * 121). "Want to Play" is anticipation — it drives the unreleased "Most anticipated" shelf; "Playing" is
 * who is in a game right now — it drives the already-released "Popular this month" shelf. Full IGDB set:
 * 1 = Visits, 2 = Want to Play, 3 = Playing, 4 = Played. Steam-source signals also exist (e.g.
 * 9 = Global Top Sellers, 10 = Most Wishlisted Upcoming) but cover only Steam games. Values are not
 * comparable across types, so each lane pins a single one.
 */
private const val POPULARITY_TYPE_WANT_TO_PLAY = 2
private const val POPULARITY_TYPE_PLAYING = 3

/**
 * Size of a Discover lane's candidate pool. A few ids drop out at hydration (filtered game types, the
 * release-window filter), so this is an upper bound on what a lane shows, not an exact count.
 */
private const val POPULARITY_POOL_LIMIT = 40

/**
 * "Want to play" ranks anticipation regardless of release status, so most of a [POPULARITY_POOL_LIMIT]
 * pool is already-released games that the `first_release_date > now` filter then discards -- a pool
 * sized for the other lane starves "Most anticipated" down to a handful of survivors. IGDB's `limit`
 * caps at 500, so there is headroom to size this well above what actually needs to survive.
 */
private const val POPULARITY_POOL_LIMIT_UPCOMING = 300

/**
 * Pool size for a lane once the user's platform filter is active. The filter cannot be pushed into the
 * ranking query (see `fetchPopularityRankedGames`), so it thins an already-ranked pool: a user who owns
 * one console drops most of a [POPULARITY_POOL_LIMIT] pool and is left with a near-empty shelf. The
 * upcoming lane already fetches [POPULARITY_POOL_LIMIT_UPCOMING] to survive its release-window filter
 * and needs no separate widening.
 */
private const val POPULARITY_POOL_LIMIT_FILTERED = 300

/**
 * Page size for the platform catalogue sync. 500 is IGDB's hard `limit` cap, so the loop pages with
 * `offset` rather than assuming the catalogue fits in one response — it grows with every new console,
 * and a silent truncation would show up as platforms simply missing from the picker.
 */
private const val PLATFORM_PAGE_LIMIT = 500

class GameRepositoryImpl @Inject constructor(
    private val apiService: IgdbApiService,
    private val gameDao: GameDao,
    private val listDao: ListDao,
    private val platformDao: PlatformDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val coverImageStorage: WishlistCoverImageStorage
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
            val excludedIds = GameType.noisyTypes.joinToString(",") { it.id.toString() }
            val queryText = """
                fields name, cover.url, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, first_release_date;
                where name ~ *"$query"* & version_parent = null & game_type != ($excludedIds);
                sort hypes desc;
                limit $SUGGESTIONS_LIMIT;
            """.trimIndent()
            val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.searchGames(body)
            AppResult.success(response.map { it.toGame() })
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override suspend fun getPopularGames(platformIds: Set<Int>): AppResult<List<Game>> =
        fetchPopularityRankedGames(
            POPULARITY_TYPE_PLAYING,
            upcomingOnly = false,
            poolLimit = if (platformIds.isEmpty()) POPULARITY_POOL_LIMIT else POPULARITY_POOL_LIMIT_FILTERED,
            platformIds = platformIds
        )

    override suspend fun getUpcomingGames(platformIds: Set<Int>): AppResult<List<Game>> =
        fetchPopularityRankedGames(
            POPULARITY_TYPE_WANT_TO_PLAY,
            upcomingOnly = true,
            poolLimit = POPULARITY_POOL_LIMIT_UPCOMING,
            platformIds = platformIds
        )

    /**
     * Backs both Discover lanes. The Popularity API ranks ids only, so rank first, then hydrate on
     * /games. [upcomingOnly] is the split the design asks for: unreleased "Most anticipated" vs
     * already-released "Popular this month". The hydrate call loses the popularity order, so it is
     * restored locally. Nothing is persisted -- these are catalogue results, not the user's games.
     *
     * [platformIds] can only be applied here, on the hydrate call: `/popularity_primitives` returns
     * game ids and a score and knows nothing about platforms, so the pool is ranked before it can be
     * filtered. That is what [POPULARITY_POOL_LIMIT_FILTERED] pays for.
     */
    private suspend fun fetchPopularityRankedGames(
        popularityType: Int,
        upcomingOnly: Boolean,
        poolLimit: Int,
        platformIds: Set<Int>
    ): AppResult<List<Game>> {
        return try {
            val primitivesQuery = """
                fields game_id, value;
                where popularity_type = $popularityType;
                sort value desc;
                limit $poolLimit;
            """.trimIndent()
            val primitivesBody = primitivesQuery.toRequestBody("text/plain".toMediaTypeOrNull())
            val rankedIds = apiService.getPopularityPrimitives(primitivesBody).map { it.gameId }
            if (rankedIds.isEmpty()) return AppResult.success(emptyList())

            val excludedIds = GameType.noisyTypes.joinToString(",") { it.id.toString() }
            val idList = rankedIds.joinToString(",")
            val nowSeconds = System.currentTimeMillis() / 1000
            val releaseFilter = if (upcomingOnly) {
                "first_release_date > $nowSeconds"
            } else {
                "first_release_date != null & first_release_date <= $nowSeconds"
            }
            // An empty selection is not a filter of "no platforms" -- the clause is dropped entirely,
            // so the feed stays wide open until the user picks something in Settings. Apicalypse
            // parentheses mean "contains at least one of", which is the ownership question being asked.
            val platformFilter = if (platformIds.isEmpty()) {
                ""
            } else {
                " & platforms = (${platformIds.joinToString(",")})"
            }
            // cover != null: the feed is a grid of covers, so a game that cannot render one is no
            // use here -- and it doubles as a cheap floor that keeps most shovelware out.
            val gamesQuery = """
                fields name, url, game_type, summary, first_release_date, cover.url, total_rating, total_rating_count, aggregated_rating, hypes, platforms.name, platforms.abbreviation, platforms.generation, platforms.category, platforms.platform_family, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher;
                where id = ($idList) & game_type != ($excludedIds) & version_parent = null & cover != null & $releaseFilter$platformFilter;
                limit $poolLimit;
            """.trimIndent()
            val gamesBody = gamesQuery.toRequestBody("text/plain".toMediaTypeOrNull())
            val games = apiService.searchGames(gamesBody).map { it.toGame() }

            // /games returns ids in its own order; restore the popularity ranking.
            val rankByGameId = rankedIds.withIndex().associate { (index, id) -> id to index }
            val ranked = games.sortedBy { rankByGameId[it.id] ?: Int.MAX_VALUE }
            AppResult.success(ranked)
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

    override fun observeGameDetail(id: Int): Flow<Game?> {
        return combine(
            gameDao.observeGameById(id),
            gameDao.getGameIdsInList(WishlistConstants.DEFAULT_WISHLIST_ID)
        ) { entity, wishlistIds ->
            entity?.toGame()?.copy(isWishlisted = id in wishlistIds)
        }
    }

    override suspend fun refreshGameDetail(id: Int): AppResult<Unit> {
        return try {
            // Try local first
            val localGame = gameDao.getGameById(id)
            val isWishlisted = gameDao.isGameInList(id, WishlistConstants.DEFAULT_WISHLIST_ID)

            val game = if (localGame != null) {
                localGame.toGame().copy(isWishlisted = isWishlisted)
            } else {
                // Fetch from network
                val queryText = """
                    fields name, url, game_type, summary, first_release_date, cover.url, total_rating, aggregated_rating, hypes, total_rating_count, platforms.name, platforms.abbreviation, platforms.generation, platforms.category, platforms.platform_family, release_dates.date, release_dates.platform.name, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, game_engines.name,
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
            AppResult.success(Unit)
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

    override fun getSavedGames(): Flow<List<Game>> {
        return combine(
            gameDao.getSavedGames(),
            gameDao.getGameIdsInList(WishlistConstants.DEFAULT_WISHLIST_ID)
        ) { entities, wishlistIds ->
            entities.map { it.toGame().copy(isWishlisted = it.game.id in wishlistIds) }
        }
    }

    override fun getKnownPlatforms(): Flow<List<Platform>> {
        return platformDao.getKnownPlatforms().map { entities ->
            entities.map { it.toPlatform() }
        }
    }

    override suspend fun syncPlatformCatalog(): AppResult<Unit> {
        return try {
            var offset = 0
            while (true) {
                val queryText = """
                    fields name, abbreviation, generation, category, platform_family;
                    sort id asc;
                    limit $PLATFORM_PAGE_LIMIT;
                    offset $offset;
                """.trimIndent()
                val body = queryText.toRequestBody("text/plain".toMediaTypeOrNull())
                val page = apiService.getPlatforms(body)
                if (page.isEmpty()) break

                platformDao.insertPlatforms(page.map { it.toPlatform().toEntity() })
                if (page.size < PLATFORM_PAGE_LIMIT) break
                offset += PLATFORM_PAGE_LIMIT
            }
            AppResult.success(Unit)
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override fun getOwnedPlatformIds(): Flow<Set<Int>> {
        return platformDao.observeOwnedPlatformIds().map { it.toSet() }
    }

    override suspend fun setOwnedPlatforms(platformIds: Set<Int>) {
        platformDao.setOwnedPlatforms(platformIds)
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
            engines = game.toEngineEntities(),
            engineCrossRefs = game.toGameEngineCrossRefs(),
            artworks = game.toArtworkEntities(),
            relatedGames = game.toRelatedGameEntities()
        )
    }

    override fun getAllLists(): Flow<List<WishlistList>> {
        return listDao.getAllLists().map { entities ->
            entities.map { it.toWishlistList() }
        }
    }

    override fun observeListById(listId: Long): Flow<WishlistList?> {
        return listDao.observeListById(listId).map { it?.toWishlistList() }
    }

    override fun getListIdsForGame(gameId: Int): Flow<List<Long>> {
        return gameDao.getListIdsForGame(gameId)
    }

    override suspend fun createList(
        name: String,
        description: String,
        icon: WishlistIcon?,
        coverImageUri: String?
    ): AppResult<Unit> {
        val coverImagePath = coverImageUri?.let { coverImageStorage.persist(it) }
        listDao.insertList(
            ListEntity(
                name = name,
                description = description,
                icon = icon,
                coverImagePath = coverImagePath
            )
        )
        return if (coverImageUri != null && coverImagePath == null) {
            AppResult.failure(RepositoryError.FileStorage)
        } else {
            AppResult.success(Unit)
        }
    }

    override suspend fun deleteList(listId: Long) {
        val list = listDao.getListById(listId) ?: return
        // The row goes first: an orphaned file is invisible, whereas a surviving row whose
        // cover file is already gone would render as a broken list.
        listDao.deleteListWithGameRefs(list)
        list.coverImagePath?.let { coverImageStorage.delete(it) }
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
