package com.example.gameswishlist.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameswishlist.core.database.GamesWishlistDatabase
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import com.example.gameswishlist.core.database.dao.SearchHistoryDao
import com.example.gameswishlist.core.model.WishlistConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.gameswishlist.core.database.R as DatabaseR

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GamesWishlistDatabase {
        return Room.databaseBuilder(
            context,
            GamesWishlistDatabase::class.java,
            GamesWishlistDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val defaultName = context.getString(DatabaseR.string.default_wishlist_name)
                    val defaultDescription =
                        context.getString(DatabaseR.string.default_wishlist_description)
                    db.execSQL(
                        "INSERT INTO wishlists (id, name, description) VALUES (${WishlistConstants.DEFAULT_WISHLIST_ID}, '$defaultName', '$defaultDescription')"
                    )
                }
            })
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideGameDao(database: GamesWishlistDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideListDao(database: GamesWishlistDatabase): ListDao {
        return database.listDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: GamesWishlistDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }
}
