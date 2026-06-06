package com.example.gameswishlist.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.gameswishlist.core.database.GamesWishlistDatabase
import com.example.gameswishlist.core.database.dao.GameDao
import com.example.gameswishlist.core.database.dao.ListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GamesWishlistDatabase {
        return Room.databaseBuilder(
            context,
            GamesWishlistDatabase::class.java,
            "games_wishlist_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideGameDao(database: GamesWishlistDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideListDao(database: GamesWishlistDatabase): ListDao {
        return database.listDao()
    }
}
