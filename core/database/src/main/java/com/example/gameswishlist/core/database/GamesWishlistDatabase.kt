package com.example.gameswishlist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.util.Converters

@Database(
    entities = [
        GameEntity::class,
        ListEntity::class,
        GameListCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GamesWishlistDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun listDao(): ListDao
}
