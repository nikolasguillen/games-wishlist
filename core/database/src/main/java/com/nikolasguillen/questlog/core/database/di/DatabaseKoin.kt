package com.nikolasguillen.questlog.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.R
import com.nikolasguillen.questlog.core.database.util.Converters
import com.nikolasguillen.questlog.core.model.WishlistIcon
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        val context = androidContext()
        Room.databaseBuilder(
            context,
            QuestLogDatabase::class.java,
            QuestLogDatabase.DATABASE_NAME
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    val defaultName = context.getString(R.string.default_wishlist_name)
                    val defaultDescription =
                        context.getString(R.string.default_wishlist_description)
                    val defaultIcon = Converters().fromWishlistIcon(WishlistIcon.HEART)
                    connection.execSQL(
                        "INSERT INTO wishlists (name, description, icon) VALUES ('$defaultName', '$defaultDescription', '$defaultIcon')"
                    )
                    // The only writer of the pointer row: it has to exist before anything reads the default.
                    connection.execSQL(
                        "INSERT INTO default_wishlist (id, listId) VALUES (0, last_insert_rowid())"
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

    single { get<QuestLogDatabase>().gameDao() }
    single { get<QuestLogDatabase>().listDao() }
    single { get<QuestLogDatabase>().platformDao() }
    single { get<QuestLogDatabase>().searchHistoryDao() }
    single { get<QuestLogDatabase>().translationDao() }
    single { get<QuestLogDatabase>().discoverCacheDao() }
    single { get<QuestLogDatabase>().releaseNotificationDao() }
}
