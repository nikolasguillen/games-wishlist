package com.example.gameswishlist.core.domain.repository

import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.model.WishlistList
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun searchGames(query: String): AppResult<List<Game>>
    suspend fun getRemoteSearchSuggestions(query: String): AppResult<List<Game>>

    /**
     * Games currently trending on IGDB, ranked by its Popularity API. Feeds the cold-start Discover
     * lane, before any taste profile is applied. The result is not persisted — these are catalogue
     * browsing results, not the user's own games, and must not pollute the games cache.
     */
    suspend fun getPopularGames(): AppResult<List<Game>>
    suspend fun addSearchToHistory(query: String)
    fun getRecentSearchHistory(): Flow<List<String>>
    suspend fun getFilteredSearchHistory(query: String): List<String>
    suspend fun deleteSearchHistoryItem(query: String)
    suspend fun clearSearchHistory()
    fun observeGameDetail(id: Int): Flow<Game?>
    suspend fun refreshGameDetail(id: Int): AppResult<Unit>
    fun getRecentlyViewedGames(): Flow<List<Game>>
    suspend fun removeRecentGame(gameId: Int)
    suspend fun clearRecentGames()
    fun getWishlistedGames(): Flow<List<Game>>
    suspend fun toggleWishlist(game: Game)
    suspend fun updateGameDetails(game: Game)

    /**
     * Every game the user acted on: in any list, or carrying a status or a priority. Wider than
     * [getWishlistedGames], which is the default list only.
     */
    fun getSavedGames(): Flow<List<Game>>
    /** Every platform the app has cached so far — not the full IGDB catalogue. */
    fun getKnownPlatforms(): Flow<List<Platform>>
    /** The platforms carried by the user's saved games; the default when no override is set. */
    fun getInferredPlatforms(): Flow<List<Platform>>
    /** The user's explicit selection. Emits an empty set when they never made one. */
    fun getOwnedPlatformIds(): Flow<Set<Int>>
    /** Replaces the selection wholesale; an empty [platformIds] clears the override. */
    suspend fun setOwnedPlatforms(platformIds: Set<Int>)

    fun getAllLists(): Flow<List<WishlistList>>
    /** Emits `null` when no list with [listId] exists (e.g. it was deleted). */
    fun observeListById(listId: Long): Flow<WishlistList?>
    fun getListIdsForGame(gameId: Int): Flow<List<Long>>
    /**
     * The list itself is always created; a [AppResult.Failure] only indicates that
     * [coverImageUri] was provided but failed to be persisted.
     */
    suspend fun createList(
        name: String,
        description: String,
        icon: WishlistIcon? = null,
        coverImageUri: String? = null
    ): AppResult<Unit>
    /**
     * Deletes the list along with its game references and its cover image file, if any.
     * Deleting an unknown [listId] is a no-op.
     */
    suspend fun deleteList(listId: Long)
    suspend fun addGameToList(gameId: Int, listId: Long)
    suspend fun removeGameFromList(gameId: Int, listId: Long)
    fun getGamesByList(listId: Long): Flow<List<Game>>
}