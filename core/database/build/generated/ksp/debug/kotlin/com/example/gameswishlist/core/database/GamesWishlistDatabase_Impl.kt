package com.example.gameswishlist.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.GameDao_Impl
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.ListDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class GamesWishlistDatabase_Impl : GamesWishlistDatabase() {
  private val _gameDao: Lazy<GameDao> = lazy {
    GameDao_Impl(this)
  }

  private val _listDao: Lazy<ListDao> = lazy {
    ListDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "17421cf15967832957ffb16541fbfe40", "12521377abe8e1d862792fbf92fa5309") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `games` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `released` TEXT, `backgroundImage` TEXT, `rating` REAL NOT NULL, `metacritic` INTEGER, `platforms` TEXT NOT NULL, `genres` TEXT NOT NULL, `publishers` TEXT NOT NULL, `developers` TEXT NOT NULL, `isWishlisted` INTEGER NOT NULL, `notes` TEXT NOT NULL, `priority` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `wishlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `game_list_cross_ref` (`gameId` INTEGER NOT NULL, `listId` INTEGER NOT NULL, PRIMARY KEY(`gameId`, `listId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '17421cf15967832957ffb16541fbfe40')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `games`")
        connection.execSQL("DROP TABLE IF EXISTS `wishlists`")
        connection.execSQL("DROP TABLE IF EXISTS `game_list_cross_ref`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsGames: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGames.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("released", TableInfo.Column("released", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("backgroundImage", TableInfo.Column("backgroundImage", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("rating", TableInfo.Column("rating", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("metacritic", TableInfo.Column("metacritic", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("platforms", TableInfo.Column("platforms", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("genres", TableInfo.Column("genres", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("publishers", TableInfo.Column("publishers", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("developers", TableInfo.Column("developers", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("isWishlisted", TableInfo.Column("isWishlisted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("priority", TableInfo.Column("priority", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGames.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGames: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGames: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGames: TableInfo = TableInfo("games", _columnsGames, _foreignKeysGames,
            _indicesGames)
        val _existingGames: TableInfo = read(connection, "games")
        if (!_infoGames.equals(_existingGames)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |games(com.example.gameswishlist.core.database.entity.GameEntity).
              | Expected:
              |""".trimMargin() + _infoGames + """
              |
              | Found:
              |""".trimMargin() + _existingGames)
        }
        val _columnsWishlists: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWishlists.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWishlists.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWishlists.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWishlists: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWishlists: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWishlists: TableInfo = TableInfo("wishlists", _columnsWishlists,
            _foreignKeysWishlists, _indicesWishlists)
        val _existingWishlists: TableInfo = read(connection, "wishlists")
        if (!_infoWishlists.equals(_existingWishlists)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |wishlists(com.example.gameswishlist.core.database.entity.ListEntity).
              | Expected:
              |""".trimMargin() + _infoWishlists + """
              |
              | Found:
              |""".trimMargin() + _existingWishlists)
        }
        val _columnsGameListCrossRef: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGameListCrossRef.put("gameId", TableInfo.Column("gameId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGameListCrossRef.put("listId", TableInfo.Column("listId", "INTEGER", true, 2, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGameListCrossRef: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGameListCrossRef: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGameListCrossRef: TableInfo = TableInfo("game_list_cross_ref",
            _columnsGameListCrossRef, _foreignKeysGameListCrossRef, _indicesGameListCrossRef)
        val _existingGameListCrossRef: TableInfo = read(connection, "game_list_cross_ref")
        if (!_infoGameListCrossRef.equals(_existingGameListCrossRef)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |game_list_cross_ref(com.example.gameswishlist.core.database.entity.GameListCrossRef).
              | Expected:
              |""".trimMargin() + _infoGameListCrossRef + """
              |
              | Found:
              |""".trimMargin() + _existingGameListCrossRef)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "games", "wishlists",
        "game_list_cross_ref")
  }

  public override fun clearAllTables() {
    super.performClear(false, "games", "wishlists", "game_list_cross_ref")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(GameDao::class, GameDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ListDao::class, ListDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun gameDao(): GameDao = _gameDao.value

  public override fun listDao(): ListDao = _listDao.value
}
