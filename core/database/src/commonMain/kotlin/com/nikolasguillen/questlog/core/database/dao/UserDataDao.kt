package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails
import com.nikolasguillen.questlog.core.database.userdata.UserDataRows

/**
 * Reads, and later replaces, all the user's data in one transaction for the cloud backup.
 *
 * An abstract class and not an interface like the other DAOs: a transaction here has to call [GameDao.saveGame], and an
 * interface DAO cannot reach another DAO. Room gives an abstract DAO the database it is built from.
 */
@Dao
abstract class UserDataDao(database: QuestLogDatabase) {

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
}
