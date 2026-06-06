package com.example.gameswishlist.core.database.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.util.Converters
import com.example.gameswishlist.core.model.GameStatus
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class GameDao_Impl(
  __db: RoomDatabase,
) : GameDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGameEntity: EntityInsertAdapter<GameEntity>

  private val __converters: Converters = Converters()

  private val __insertAdapterOfGameListCrossRef: EntityInsertAdapter<GameListCrossRef>

  private val __deleteAdapterOfGameEntity: EntityDeleteOrUpdateAdapter<GameEntity>

  private val __deleteAdapterOfGameListCrossRef: EntityDeleteOrUpdateAdapter<GameListCrossRef>

  private val __updateAdapterOfGameEntity: EntityDeleteOrUpdateAdapter<GameEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfGameEntity = object : EntityInsertAdapter<GameEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `games` (`id`,`name`,`description`,`released`,`backgroundImage`,`rating`,`metacritic`,`platforms`,`genres`,`publishers`,`developers`,`isWishlisted`,`notes`,`priority`,`status`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GameEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        val _tmpReleased: String? = entity.released
        if (_tmpReleased == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpReleased)
        }
        val _tmpBackgroundImage: String? = entity.backgroundImage
        if (_tmpBackgroundImage == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpBackgroundImage)
        }
        statement.bindDouble(6, entity.rating)
        val _tmpMetacritic: Int? = entity.metacritic
        if (_tmpMetacritic == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpMetacritic.toLong())
        }
        statement.bindText(8, entity.platforms)
        statement.bindText(9, entity.genres)
        statement.bindText(10, entity.publishers)
        statement.bindText(11, entity.developers)
        val _tmp: Int = if (entity.isWishlisted) 1 else 0
        statement.bindLong(12, _tmp.toLong())
        statement.bindText(13, entity.notes)
        statement.bindLong(14, entity.priority.toLong())
        val _tmp_1: String = __converters.fromGameStatus(entity.status)
        statement.bindText(15, _tmp_1)
      }
    }
    this.__insertAdapterOfGameListCrossRef = object : EntityInsertAdapter<GameListCrossRef>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `game_list_cross_ref` (`gameId`,`listId`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GameListCrossRef) {
        statement.bindLong(1, entity.gameId.toLong())
        statement.bindLong(2, entity.listId)
      }
    }
    this.__deleteAdapterOfGameEntity = object : EntityDeleteOrUpdateAdapter<GameEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `games` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: GameEntity) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__deleteAdapterOfGameListCrossRef = object :
        EntityDeleteOrUpdateAdapter<GameListCrossRef>() {
      protected override fun createQuery(): String =
          "DELETE FROM `game_list_cross_ref` WHERE `gameId` = ? AND `listId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: GameListCrossRef) {
        statement.bindLong(1, entity.gameId.toLong())
        statement.bindLong(2, entity.listId)
      }
    }
    this.__updateAdapterOfGameEntity = object : EntityDeleteOrUpdateAdapter<GameEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `games` SET `id` = ?,`name` = ?,`description` = ?,`released` = ?,`backgroundImage` = ?,`rating` = ?,`metacritic` = ?,`platforms` = ?,`genres` = ?,`publishers` = ?,`developers` = ?,`isWishlisted` = ?,`notes` = ?,`priority` = ?,`status` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: GameEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        val _tmpReleased: String? = entity.released
        if (_tmpReleased == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpReleased)
        }
        val _tmpBackgroundImage: String? = entity.backgroundImage
        if (_tmpBackgroundImage == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpBackgroundImage)
        }
        statement.bindDouble(6, entity.rating)
        val _tmpMetacritic: Int? = entity.metacritic
        if (_tmpMetacritic == null) {
          statement.bindNull(7)
        } else {
          statement.bindLong(7, _tmpMetacritic.toLong())
        }
        statement.bindText(8, entity.platforms)
        statement.bindText(9, entity.genres)
        statement.bindText(10, entity.publishers)
        statement.bindText(11, entity.developers)
        val _tmp: Int = if (entity.isWishlisted) 1 else 0
        statement.bindLong(12, _tmp.toLong())
        statement.bindText(13, entity.notes)
        statement.bindLong(14, entity.priority.toLong())
        val _tmp_1: String = __converters.fromGameStatus(entity.status)
        statement.bindText(15, _tmp_1)
        statement.bindLong(16, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertGame(game: GameEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfGameEntity.insert(_connection, game)
  }

  public override suspend fun insertGameListCrossRef(crossRef: GameListCrossRef): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGameListCrossRef.insert(_connection, crossRef)
  }

  public override suspend fun deleteGame(game: GameEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfGameEntity.handle(_connection, game)
  }

  public override suspend fun deleteGameListCrossRef(crossRef: GameListCrossRef): Unit =
      performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfGameListCrossRef.handle(_connection, crossRef)
  }

  public override suspend fun updateGame(game: GameEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfGameEntity.handle(_connection, game)
  }

  public override fun getWishlistedGames(): Flow<List<GameEntity>> {
    val _sql: String = "SELECT * FROM games WHERE isWishlisted = 1"
    return createFlow(__db, false, arrayOf("games")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfReleased: Int = getColumnIndexOrThrow(_stmt, "released")
        val _columnIndexOfBackgroundImage: Int = getColumnIndexOrThrow(_stmt, "backgroundImage")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfMetacritic: Int = getColumnIndexOrThrow(_stmt, "metacritic")
        val _columnIndexOfPlatforms: Int = getColumnIndexOrThrow(_stmt, "platforms")
        val _columnIndexOfGenres: Int = getColumnIndexOrThrow(_stmt, "genres")
        val _columnIndexOfPublishers: Int = getColumnIndexOrThrow(_stmt, "publishers")
        val _columnIndexOfDevelopers: Int = getColumnIndexOrThrow(_stmt, "developers")
        val _columnIndexOfIsWishlisted: Int = getColumnIndexOrThrow(_stmt, "isWishlisted")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<GameEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GameEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpReleased: String?
          if (_stmt.isNull(_columnIndexOfReleased)) {
            _tmpReleased = null
          } else {
            _tmpReleased = _stmt.getText(_columnIndexOfReleased)
          }
          val _tmpBackgroundImage: String?
          if (_stmt.isNull(_columnIndexOfBackgroundImage)) {
            _tmpBackgroundImage = null
          } else {
            _tmpBackgroundImage = _stmt.getText(_columnIndexOfBackgroundImage)
          }
          val _tmpRating: Double
          _tmpRating = _stmt.getDouble(_columnIndexOfRating)
          val _tmpMetacritic: Int?
          if (_stmt.isNull(_columnIndexOfMetacritic)) {
            _tmpMetacritic = null
          } else {
            _tmpMetacritic = _stmt.getLong(_columnIndexOfMetacritic).toInt()
          }
          val _tmpPlatforms: String
          _tmpPlatforms = _stmt.getText(_columnIndexOfPlatforms)
          val _tmpGenres: String
          _tmpGenres = _stmt.getText(_columnIndexOfGenres)
          val _tmpPublishers: String
          _tmpPublishers = _stmt.getText(_columnIndexOfPublishers)
          val _tmpDevelopers: String
          _tmpDevelopers = _stmt.getText(_columnIndexOfDevelopers)
          val _tmpIsWishlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWishlisted).toInt()
          _tmpIsWishlisted = _tmp != 0
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpStatus: GameStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __converters.toGameStatus(_tmp_1)
          _item =
              GameEntity(_tmpId,_tmpName,_tmpDescription,_tmpReleased,_tmpBackgroundImage,_tmpRating,_tmpMetacritic,_tmpPlatforms,_tmpGenres,_tmpPublishers,_tmpDevelopers,_tmpIsWishlisted,_tmpNotes,_tmpPriority,_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getGameById(id: Int): GameEntity? {
    val _sql: String = "SELECT * FROM games WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfReleased: Int = getColumnIndexOrThrow(_stmt, "released")
        val _columnIndexOfBackgroundImage: Int = getColumnIndexOrThrow(_stmt, "backgroundImage")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfMetacritic: Int = getColumnIndexOrThrow(_stmt, "metacritic")
        val _columnIndexOfPlatforms: Int = getColumnIndexOrThrow(_stmt, "platforms")
        val _columnIndexOfGenres: Int = getColumnIndexOrThrow(_stmt, "genres")
        val _columnIndexOfPublishers: Int = getColumnIndexOrThrow(_stmt, "publishers")
        val _columnIndexOfDevelopers: Int = getColumnIndexOrThrow(_stmt, "developers")
        val _columnIndexOfIsWishlisted: Int = getColumnIndexOrThrow(_stmt, "isWishlisted")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: GameEntity?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpReleased: String?
          if (_stmt.isNull(_columnIndexOfReleased)) {
            _tmpReleased = null
          } else {
            _tmpReleased = _stmt.getText(_columnIndexOfReleased)
          }
          val _tmpBackgroundImage: String?
          if (_stmt.isNull(_columnIndexOfBackgroundImage)) {
            _tmpBackgroundImage = null
          } else {
            _tmpBackgroundImage = _stmt.getText(_columnIndexOfBackgroundImage)
          }
          val _tmpRating: Double
          _tmpRating = _stmt.getDouble(_columnIndexOfRating)
          val _tmpMetacritic: Int?
          if (_stmt.isNull(_columnIndexOfMetacritic)) {
            _tmpMetacritic = null
          } else {
            _tmpMetacritic = _stmt.getLong(_columnIndexOfMetacritic).toInt()
          }
          val _tmpPlatforms: String
          _tmpPlatforms = _stmt.getText(_columnIndexOfPlatforms)
          val _tmpGenres: String
          _tmpGenres = _stmt.getText(_columnIndexOfGenres)
          val _tmpPublishers: String
          _tmpPublishers = _stmt.getText(_columnIndexOfPublishers)
          val _tmpDevelopers: String
          _tmpDevelopers = _stmt.getText(_columnIndexOfDevelopers)
          val _tmpIsWishlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWishlisted).toInt()
          _tmpIsWishlisted = _tmp != 0
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpStatus: GameStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __converters.toGameStatus(_tmp_1)
          _result =
              GameEntity(_tmpId,_tmpName,_tmpDescription,_tmpReleased,_tmpBackgroundImage,_tmpRating,_tmpMetacritic,_tmpPlatforms,_tmpGenres,_tmpPublishers,_tmpDevelopers,_tmpIsWishlisted,_tmpNotes,_tmpPriority,_tmpStatus)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getGamesByListId(listId: Long): Flow<List<GameEntity>> {
    val _sql: String =
        "SELECT * FROM games INNER JOIN game_list_cross_ref ON games.id = game_list_cross_ref.gameId WHERE game_list_cross_ref.listId = ?"
    return createFlow(__db, false, arrayOf("games", "game_list_cross_ref")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, listId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfReleased: Int = getColumnIndexOrThrow(_stmt, "released")
        val _columnIndexOfBackgroundImage: Int = getColumnIndexOrThrow(_stmt, "backgroundImage")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfMetacritic: Int = getColumnIndexOrThrow(_stmt, "metacritic")
        val _columnIndexOfPlatforms: Int = getColumnIndexOrThrow(_stmt, "platforms")
        val _columnIndexOfGenres: Int = getColumnIndexOrThrow(_stmt, "genres")
        val _columnIndexOfPublishers: Int = getColumnIndexOrThrow(_stmt, "publishers")
        val _columnIndexOfDevelopers: Int = getColumnIndexOrThrow(_stmt, "developers")
        val _columnIndexOfIsWishlisted: Int = getColumnIndexOrThrow(_stmt, "isWishlisted")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _result: MutableList<GameEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GameEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpReleased: String?
          if (_stmt.isNull(_columnIndexOfReleased)) {
            _tmpReleased = null
          } else {
            _tmpReleased = _stmt.getText(_columnIndexOfReleased)
          }
          val _tmpBackgroundImage: String?
          if (_stmt.isNull(_columnIndexOfBackgroundImage)) {
            _tmpBackgroundImage = null
          } else {
            _tmpBackgroundImage = _stmt.getText(_columnIndexOfBackgroundImage)
          }
          val _tmpRating: Double
          _tmpRating = _stmt.getDouble(_columnIndexOfRating)
          val _tmpMetacritic: Int?
          if (_stmt.isNull(_columnIndexOfMetacritic)) {
            _tmpMetacritic = null
          } else {
            _tmpMetacritic = _stmt.getLong(_columnIndexOfMetacritic).toInt()
          }
          val _tmpPlatforms: String
          _tmpPlatforms = _stmt.getText(_columnIndexOfPlatforms)
          val _tmpGenres: String
          _tmpGenres = _stmt.getText(_columnIndexOfGenres)
          val _tmpPublishers: String
          _tmpPublishers = _stmt.getText(_columnIndexOfPublishers)
          val _tmpDevelopers: String
          _tmpDevelopers = _stmt.getText(_columnIndexOfDevelopers)
          val _tmpIsWishlisted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsWishlisted).toInt()
          _tmpIsWishlisted = _tmp != 0
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_columnIndexOfPriority).toInt()
          val _tmpStatus: GameStatus
          val _tmp_1: String
          _tmp_1 = _stmt.getText(_columnIndexOfStatus)
          _tmpStatus = __converters.toGameStatus(_tmp_1)
          _item =
              GameEntity(_tmpId,_tmpName,_tmpDescription,_tmpReleased,_tmpBackgroundImage,_tmpRating,_tmpMetacritic,_tmpPlatforms,_tmpGenres,_tmpPublishers,_tmpDevelopers,_tmpIsWishlisted,_tmpNotes,_tmpPriority,_tmpStatus)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
