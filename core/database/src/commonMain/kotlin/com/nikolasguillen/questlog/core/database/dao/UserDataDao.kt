package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.entity.DefaultWishlistEntity
import com.nikolasguillen.questlog.core.database.entity.GameEngineCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.OwnedPlatformEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails
import com.nikolasguillen.questlog.core.database.userdata.UserDataRows

/**
 * Reads and replaces all the user's data, each in one transaction, for the cloud backup.
 *
 * An abstract class and not an interface like the other DAOs: a transaction here has to call [GameDao.saveGame], and an
 * interface DAO cannot reach another DAO. Room gives an abstract DAO the database it is built from.
 */
@Dao
abstract class UserDataDao(private val database: QuestLogDatabase) {

    /** One consistent snapshot: a write between two of the reads would otherwise tear a game from its list. */
    @Transaction
    open suspend fun readUserData(): UserDataRows = UserDataRows(
        lists = readLists(),
        defaultListId = readDefaultListId(),
        memberships = readMemberships(),
        ownedPlatformIds = readOwnedPlatformIds(),
        ownedPlatforms = readOwnedPlatforms(),
        savedGames = readSavedGames()
    )

    /**
     * Makes the user's data exactly [rows], all or nothing: the clears and the writes share one transaction, so a failure
     * anywhere leaves the database as it was.
     *
     * The `default_wishlist` row is deleted first and inserted after the lists, because its foreign key onto the lists
     * is `RESTRICT`. Every game is written through [GameDao.saveGame], the only write path for a game, which is why this
     * is an abstract class that can reach the other DAO. A game the device already has keeps its own `lastViewedAt`.
     *
     * A restore does **not** touch search history, `lastViewedAt`, the translations, the Discover caches or the release
     * reminders: those are not the user's data (FR-022). Games that are cached but not in [rows] stay, and lose their
     * status, priority and notes, because those belong to the data being replaced.
     */
    @Transaction
    open suspend fun replaceUserData(rows: UserDataRows) {
        deleteDefaultList()
        deleteMemberships()
        deleteLists()
        deleteOwnedPlatforms()
        clearUserFieldsOnGames()

        insertLists(rows.lists)
        insertDefaultList(DefaultWishlistEntity(id = 0, listId = rows.defaultListId))

        val gameDao = database.gameDao()
        rows.savedGames.forEach { saved ->
            gameDao.saveGame(
                game = saved.game.copy(lastViewedAt = readLastViewedAt(saved.game.id)),
                platforms = saved.platformRefs.map { it.platform },
                platformCrossRefs = saved.platformRefs.map { it.crossRef },
                genres = saved.genres,
                genreCrossRefs = saved.genres.map { GameGenreCrossRef(gameId = saved.game.id, genreId = it.id) },
                companies = saved.companyRefs.map { it.company },
                companyCrossRefs = saved.companyRefs.map { it.crossRef },
                engines = saved.engines,
                engineCrossRefs = saved.engines.map { GameEngineCrossRef(gameId = saved.game.id, engineId = it.id) },
                artworks = saved.artworks,
                relatedGames = saved.relatedGames
            )
        }

        insertMemberships(rows.memberships)
        upsertPlatforms(rows.ownedPlatforms)
        insertOwnedPlatforms(rows.ownedPlatformIds.map { OwnedPlatformEntity(platformId = it) })
    }

    /**
     * Whether there is anything the user made. A fresh install's seeded default wishlist is not: backing up a device
     * that holds only that would replace a real backup with an empty one. So it is a saved game, **or more than one
     * list**, or an owned platform.
     */
    @Query(
        "SELECT EXISTS(" +
                "SELECT 1 FROM games LEFT JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId " +
                "WHERE game_list_cross_ref.gameId IS NOT NULL OR games.status IS NOT NULL OR games.priority IS NOT NULL" +
                ") OR (SELECT COUNT(*) FROM wishlists) > 1 OR EXISTS(SELECT 1 FROM owned_platforms)"
    )
    abstract suspend fun hasUserData(): Boolean

    @Query("SELECT * FROM wishlists")
    protected abstract suspend fun readLists(): List<ListEntity>

    @Query("SELECT listId FROM default_wishlist WHERE id = 0")
    protected abstract suspend fun readDefaultListId(): Long

    @Query("SELECT * FROM game_list_cross_ref")
    protected abstract suspend fun readMemberships(): List<GameListCrossRef>

    @Query("SELECT platformId FROM owned_platforms")
    protected abstract suspend fun readOwnedPlatformIds(): List<Int>

    @Query("SELECT * FROM platforms WHERE id IN (SELECT platformId FROM owned_platforms)")
    protected abstract suspend fun readOwnedPlatforms(): List<PlatformEntity>

    /** The same predicate as [GameDao.getSavedGames]: in some list, or with a status, or with a priority. */
    @Transaction
    @Query(
        "SELECT DISTINCT games.* FROM games " +
                "LEFT JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId " +
                "WHERE game_list_cross_ref.gameId IS NOT NULL " +
                "OR games.status IS NOT NULL OR games.priority IS NOT NULL"
    )
    protected abstract suspend fun readSavedGames(): List<GameWithAllDetails>

    /** The cover files the lists point at, so a restore can sweep the ones nothing references and, later, the replaced ones. */
    @Query("SELECT coverImagePath FROM wishlists WHERE coverImagePath IS NOT NULL")
    abstract suspend fun readCoverPaths(): List<String>

    @Query("DELETE FROM default_wishlist")
    protected abstract suspend fun deleteDefaultList()

    @Query("DELETE FROM game_list_cross_ref")
    protected abstract suspend fun deleteMemberships()

    @Query("DELETE FROM wishlists")
    protected abstract suspend fun deleteLists()

    @Query("DELETE FROM owned_platforms")
    protected abstract suspend fun deleteOwnedPlatforms()

    @Query("UPDATE games SET status = NULL, priority = NULL, notes = ''")
    protected abstract suspend fun clearUserFieldsOnGames()

    @Query("SELECT lastViewedAt FROM games WHERE id = :gameId")
    protected abstract suspend fun readLastViewedAt(gameId: Int): Long?

    // The lists keep the ids they had, so the memberships and the default pointer still line up.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertLists(lists: List<ListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDefaultList(defaultList: DefaultWishlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMemberships(memberships: List<GameListCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertPlatforms(platforms: List<PlatformEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOwnedPlatforms(owned: List<OwnedPlatformEntity>)
}
