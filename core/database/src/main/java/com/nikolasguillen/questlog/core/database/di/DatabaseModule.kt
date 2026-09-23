package com.nikolasguillen.questlog.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.R
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.database.util.Converters
import com.nikolasguillen.questlog.core.model.WishlistConstants
import com.nikolasguillen.questlog.core.model.WishlistIcon
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
    fun provideDatabase(@ApplicationContext context: Context): QuestLogDatabase {
        return Room.databaseBuilder(
            context,
            QuestLogDatabase::class.java,
            QuestLogDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val defaultName = context.getString(R.string.default_wishlist_name)
                    val defaultDescription =
                        context.getString(R.string.default_wishlist_description)
                    val defaultIcon = Converters().fromWishlistIcon(WishlistIcon.HEART)
                    db.execSQL(
                        "INSERT INTO wishlists (id, name, description, icon) VALUES (${WishlistConstants.DEFAULT_WISHLIST_ID}, '$defaultName', '$defaultDescription', '$defaultIcon')"
                    )
                }
            })
            // The app is not published, so there are no installs whose data is worth preserving: the
            // database stays at version 1 and every entity change simply recreates it. Migrations start
            // when the owner says the app ships — see docs/tech-debt.md. Until then, do not bump the
            // version to work around this, because that is what makes a migration mandatory.
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideGameDao(database: QuestLogDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    fun provideListDao(database: QuestLogDatabase): ListDao {
        return database.listDao()
    }

    @Provides
    fun providePlatformDao(database: QuestLogDatabase): PlatformDao {
        return database.platformDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: QuestLogDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideTranslationDao(database: QuestLogDatabase): TranslationDao {
        return database.translationDao()
    }
}
