package com.example.gameswishlist.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.EngineEntity
import com.example.gameswishlist.core.database.entity.GameArtworkEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEngineCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.database.entity.RelatedGameEntity
import com.example.gameswishlist.core.database.relation.GameWithAllDetails
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
    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGameById(id: Int): Flow<GameWithAllDetails?>

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

    @Query("SELECT listId FROM game_list_cross_ref WHERE gameId = :gameId")
    fun getListIdsForGame(gameId: Int): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameListCrossRef(crossRef: GameListCrossRef)

    @Delete
    suspend fun deleteGameListCrossRef(crossRef: GameListCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlatformCrossRef(crossRef: GamePlatformCrossRef)

    @Query("DELETE FROM game_platform_cross_ref WHERE gameId = :gameId")
    suspend fun deletePlatformRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenre(genre: GenreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameGenreCrossRef(crossRef: GameGenreCrossRef)

    @Query("DELETE FROM game_genre_cross_ref WHERE gameId = :gameId")
    suspend fun deleteGenreRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameCompanyCrossRef(crossRef: GameCompanyCrossRef)

    @Query("DELETE FROM game_company_cross_ref WHERE gameId = :gameId")
    suspend fun deleteCompanyRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEngine(engine: EngineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameEngineCrossRef(crossRef: GameEngineCrossRef)

    @Query("DELETE FROM game_engine_cross_ref WHERE gameId = :gameId")
    suspend fun deleteEngineRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameArtwork(artwork: GameArtworkEntity)

    @Query("DELETE FROM game_artworks WHERE gameId = :gameId")
    suspend fun deleteArtworksByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelatedGame(relatedGame: RelatedGameEntity)

    @Query("DELETE FROM related_games WHERE parentId = :parentId")
    suspend fun deleteRelatedGamesByParentId(parentId: Int)

    @Transaction
    suspend fun saveGame(
        game: GameEntity,
        platforms: List<PlatformEntity>,
        platformCrossRefs: List<GamePlatformCrossRef>,
        genres: List<GenreEntity>,
        genreCrossRefs: List<GameGenreCrossRef>,
        companies: List<CompanyEntity>,
        companyCrossRefs: List<GameCompanyCrossRef>,
        engines: List<EngineEntity> = emptyList(),
        engineCrossRefs: List<GameEngineCrossRef> = emptyList(),
        artworks: List<GameArtworkEntity> = emptyList(),
        relatedGames: List<RelatedGameEntity> = emptyList()
    ) {
        insertGame(game)

        // The cross-refs and the child rows are cleared first because they are keyed by the game, not by a
        // stable id of their own: a game that lost a genre, a platform, a publisher or an engine upstream,
        // or whose gallery shrank, would otherwise keep the leftovers forever — a REPLACE insert only
        // overwrites the rows that came back. The lookup tables are shared between games and stay.
        deletePlatformRefsByGameId(game.id)
        platforms.forEach { insertPlatform(it) }
        platformCrossRefs.forEach { insertGamePlatformCrossRef(it) }

        deleteGenreRefsByGameId(game.id)
        genres.forEach { insertGenre(it) }
        genreCrossRefs.forEach { insertGameGenreCrossRef(it) }

        deleteCompanyRefsByGameId(game.id)
        companies.forEach { insertCompany(it) }
        companyCrossRefs.forEach { insertGameCompanyCrossRef(it) }

        deleteEngineRefsByGameId(game.id)
        engines.forEach { insertEngine(it) }
        engineCrossRefs.forEach { insertGameEngineCrossRef(it) }

        deleteArtworksByGameId(game.id)
        artworks.forEach { insertGameArtwork(it) }

        deleteRelatedGamesByParentId(game.id)
        relatedGames.forEach { insertRelatedGame(it) }
    }
}
