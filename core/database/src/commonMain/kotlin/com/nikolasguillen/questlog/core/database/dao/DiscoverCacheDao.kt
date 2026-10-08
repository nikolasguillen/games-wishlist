package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nikolasguillen.questlog.core.database.entity.CachedGameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGameEntity
import com.nikolasguillen.questlog.core.database.entity.CachedGameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.DiscoverLaneCacheEntity
import com.nikolasguillen.questlog.core.database.entity.DiscoverLaneEntryEntity
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.CachedGameWithDetails
import com.nikolasguillen.questlog.core.model.DiscoverLane

@Dao
interface DiscoverCacheDao {

    @Transaction
    @Query(
        "SELECT cached_games.* FROM cached_games " +
            "INNER JOIN discover_lane_entries ON cached_games.id = discover_lane_entries.gameId " +
            "WHERE discover_lane_entries.lane = :lane " +
            "ORDER BY discover_lane_entries.position"
    )
    suspend fun getLaneGames(lane: DiscoverLane): List<CachedGameWithDetails>

    @Query("SELECT fetchedAt FROM discover_lane_cache WHERE lane = :lane")
    suspend fun getLaneFetchedAt(lane: DiscoverLane): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaneCache(cache: DiscoverLaneCacheEntity)

    @Query("DELETE FROM discover_lane_entries WHERE lane = :lane")
    suspend fun deleteEntriesForLane(lane: DiscoverLane)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<DiscoverLaneEntryEntity>)

    /**
     * `IGNORE`, not `REPLACE`: the lane hydrate query is lean and must never overwrite a richer row a
     * prior detail fetch already populated in the shared lookup table -- the same reasoning as
     * [GameDao.insertPlatformIfAbsent].
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlatformsIfAbsent(platforms: List<PlatformEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGenresIfAbsent(genres: List<GenreEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompaniesIfAbsent(companies: List<CompanyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<CachedGameEntity>)

    @Query("DELETE FROM cached_game_platform_cross_ref WHERE gameId = :gameId")
    suspend fun deletePlatformRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatformRefs(refs: List<CachedGamePlatformCrossRef>)

    @Query("DELETE FROM cached_game_genre_cross_ref WHERE gameId = :gameId")
    suspend fun deleteGenreRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenreRefs(refs: List<CachedGameGenreCrossRef>)

    @Query("DELETE FROM cached_game_company_cross_ref WHERE gameId = :gameId")
    suspend fun deleteCompanyRefsByGameId(gameId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyRefs(refs: List<CachedGameCompanyCrossRef>)

    /** A game no lane references any more is dropped, along with its cross-refs -- the shared lookups are not touched. */
    @Query("DELETE FROM cached_games WHERE id NOT IN (SELECT DISTINCT gameId FROM discover_lane_entries)")
    suspend fun pruneOrphanedGames()

    @Query("DELETE FROM cached_game_platform_cross_ref WHERE gameId NOT IN (SELECT id FROM cached_games)")
    suspend fun pruneOrphanedPlatformRefs()

    @Query("DELETE FROM cached_game_genre_cross_ref WHERE gameId NOT IN (SELECT id FROM cached_games)")
    suspend fun pruneOrphanedGenreRefs()

    @Query("DELETE FROM cached_game_company_cross_ref WHERE gameId NOT IN (SELECT id FROM cached_games)")
    suspend fun pruneOrphanedCompanyRefs()

    @Query("DELETE FROM discover_lane_cache")
    suspend fun clearLaneCache()

    @Query("DELETE FROM discover_lane_entries")
    suspend fun clearEntries()

    @Query("DELETE FROM cached_games")
    suspend fun clearGames()

    @Query("DELETE FROM cached_game_platform_cross_ref")
    suspend fun clearPlatformRefs()

    @Query("DELETE FROM cached_game_genre_cross_ref")
    suspend fun clearGenreRefs()

    @Query("DELETE FROM cached_game_company_cross_ref")
    suspend fun clearCompanyRefs()

    /**
     * Replaces one lane's cached content in a single transaction, following [GameDao.saveGame]'s
     * discipline of deleting a game's cross-refs before re-inserting them -- a `REPLACE` insert alone
     * would leave the leftovers of a shorter list behind. The shared `platforms`/`genres`/`companies`
     * lookups are inserted only if absent and are never pruned, since they are shared with the user's own
     * games. A game no lane references any more is pruned at the end, after this lane's new entries have
     * landed.
     */
    @Transaction
    suspend fun replaceLane(
        lane: DiscoverLane,
        fetchedAt: Long,
        games: List<CachedGameEntity>,
        entries: List<DiscoverLaneEntryEntity>,
        platforms: List<PlatformEntity>,
        genres: List<GenreEntity>,
        companies: List<CompanyEntity>,
        platformRefs: List<CachedGamePlatformCrossRef>,
        genreRefs: List<CachedGameGenreCrossRef>,
        companyRefs: List<CachedGameCompanyCrossRef>
    ) {
        deleteEntriesForLane(lane)

        insertPlatformsIfAbsent(platforms)
        insertGenresIfAbsent(genres)
        insertCompaniesIfAbsent(companies)

        insertGames(games)
        games.forEach { game ->
            deletePlatformRefsByGameId(game.id)
            deleteGenreRefsByGameId(game.id)
            deleteCompanyRefsByGameId(game.id)
        }
        insertPlatformRefs(platformRefs)
        insertGenreRefs(genreRefs)
        insertCompanyRefs(companyRefs)

        insertEntries(entries)
        insertLaneCache(DiscoverLaneCacheEntity(lane, fetchedAt))

        pruneOrphanedGames()
        pruneOrphanedPlatformRefs()
        pruneOrphanedGenreRefs()
        pruneOrphanedCompanyRefs()
    }

    /** Wipes every cache table. Never touches `games`, `game_list_cross_ref`, `owned_platforms` or the shared lookups. */
    @Transaction
    suspend fun clearAll() {
        clearLaneCache()
        clearEntries()
        clearGames()
        clearPlatformRefs()
        clearGenreRefs()
        clearCompanyRefs()
    }
}
