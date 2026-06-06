package com.example.gameswishlist.core.database.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.gameswishlist.core.database.entity.ListEntity
import javax.`annotation`.processing.Generated
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
public class ListDao_Impl(
  __db: RoomDatabase,
) : ListDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfListEntity: EntityInsertAdapter<ListEntity>

  private val __deleteAdapterOfListEntity: EntityDeleteOrUpdateAdapter<ListEntity>

  private val __updateAdapterOfListEntity: EntityDeleteOrUpdateAdapter<ListEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfListEntity = object : EntityInsertAdapter<ListEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `wishlists` (`id`,`name`,`description`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ListEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
      }
    }
    this.__deleteAdapterOfListEntity = object : EntityDeleteOrUpdateAdapter<ListEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `wishlists` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ListEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfListEntity = object : EntityDeleteOrUpdateAdapter<ListEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `wishlists` SET `id` = ?,`name` = ?,`description` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ListEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindLong(4, entity.id)
      }
    }
  }

  public override suspend fun insertList(list: ListEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfListEntity.insert(_connection, list)
  }

  public override suspend fun deleteList(list: ListEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfListEntity.handle(_connection, list)
  }

  public override suspend fun updateList(list: ListEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfListEntity.handle(_connection, list)
  }

  public override fun getAllLists(): Flow<List<ListEntity>> {
    val _sql: String = "SELECT * FROM wishlists"
    return createFlow(__db, false, arrayOf("wishlists")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _result: MutableList<ListEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ListEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          _item = ListEntity(_tmpId,_tmpName,_tmpDescription)
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
