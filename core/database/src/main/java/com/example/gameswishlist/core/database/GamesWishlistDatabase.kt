package com.example.gameswishlist.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GameListCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.database.entity.SearchHistoryEntity
import com.example.gameswishlist.core.database.util.Converters

@Database(
    entities = [
        SearchHistoryEntity::class,
        GameEntity::class,
        ListEntity::class,
        GameListCrossRef::class,
        PlatformEntity::class,
        GamePlatformCrossRef::class,
        GenreEntity::class,
        GameGenreCrossRef::class,
        CompanyEntity::class,
        GameCompanyCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GamesWishlistDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun listDao(): ListDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
