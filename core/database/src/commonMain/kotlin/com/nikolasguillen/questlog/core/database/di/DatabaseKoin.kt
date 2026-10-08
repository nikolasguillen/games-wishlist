package com.nikolasguillen.questlog.core.database.di

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.nikolasguillen.questlog.core.database.DefaultWishlistSeed
import com.nikolasguillen.questlog.core.database.QuestLogDatabase
import com.nikolasguillen.questlog.core.database.util.Converters
import com.nikolasguillen.questlog.core.model.WishlistIcon
import org.koin.dsl.module

/**
 * Needs a `DefaultWishlistSeed` from the app, and `databasePlatformModule` for the platform's database file.
 */
val databaseModule = module {
    single {
        val seed = get<DefaultWishlistSeed>()
        get<RoomDatabase.Builder<QuestLogDatabase>>()
            .setDriver(BundledSQLiteDriver())
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(connection: SQLiteConnection) {
                    super.onCreate(connection)
                    val defaultIcon = Converters().fromWishlistIcon(WishlistIcon.HEART)
                    connection.execSQL(
                        "INSERT INTO wishlists (name, description, icon) VALUES ('${seed.name}', '${seed.description}', '$defaultIcon')"
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
