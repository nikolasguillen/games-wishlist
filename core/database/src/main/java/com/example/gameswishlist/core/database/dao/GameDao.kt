package com.example.gameswishlist.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GameWithAllDetails
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.model.WishlistConstants
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Transaction
    @Query(
        "SELECT * FROM games " +
                "INNER JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId " +
                "WHERE game_list_cross_ref.listId = ${WishlistConstants.DEFAULT_WISHLIST_ID}"
    )
    fun getWishlistedGames(): Flow<List<GameWithAllDetails>>

    @Transaction
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): GameWithAllDetails?

    @Transaction
    @Query("SELECT * FROM games WHERE lastViewedAt IS NOT NULL ORDER BY lastViewedAt DESC LIMIT 10")
    fun getRecentlyViewedGames(): Flow<List<GameWithAllDetails>>

    @Query("UPDATE games SET lastViewedAt = NULL WHERE id = :gameId")
    suspend fun clearLastViewedAt(gameId: Int)

    @Query("UPDATE games SET lastViewedAt = NULL")
    suspend fun clearAllLastViewedAt()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Transaction
    @Query("SELECT * FROM games INNER JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId WHERE game_list_cross_ref.listId = :listId")
    fun getGamesByListId(listId: Long): Flow<List<GameWithAllDetails>>

    @Query("SELECT EXISTS(SELECT 1 FROM game_list_cross_ref WHERE gameId = :gameId AND listId = :listId)")
    suspend fun isGameInList(gameId: Int, listId: Long): Boolean

    @Query("SELECT gameId FROM game_list_cross_ref WHERE listId = :listId")
    fun getGameIdsInList(listId: Long): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameListCrossRef(crossRef: GameListCrossRef)

    @Delete
    suspend fun deleteGameListCrossRef(crossRef: GameListCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlatformCrossRef(crossRef: GamePlatformCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: GenreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameGenreCrossRef(crossRef: GameGenreCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameCompanyCrossRef(crossRef: GameCompanyCrossRef)

    @Transaction
    suspend fun saveGame(
        game: GameEntity,
        platforms: List<PlatformEntity>,
        platformCrossRefs: List<GamePlatformCrossRef>,
        genres: List<GenreEntity>,
        genreCrossRefs: List<GameGenreCrossRef>,
        companies: List<CompanyEntity>,
        companyCrossRefs: List<GameCompanyCrossRef>
    ) {
        insertGame(game)
        platforms.forEach { insertPlatform(it) }
        platformCrossRefs.forEach { insertGamePlatformCrossRef(it) }
        genres.forEach { insertGenre(it) }
        genreCrossRefs.forEach { insertGameGenreCrossRef(it) }
        companies.forEach { insertCompany(it) }
        companyCrossRefs.forEach { insertGameCompanyCrossRef(it) }
    }
}
