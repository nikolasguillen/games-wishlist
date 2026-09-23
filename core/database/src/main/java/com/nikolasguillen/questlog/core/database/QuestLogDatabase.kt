package com.nikolasguillen.questlog.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nikolasguillen.questlog.core.database.dao.GameDao
import com.nikolasguillen.questlog.core.database.dao.ListDao
import com.nikolasguillen.questlog.core.database.dao.PlatformDao
import com.nikolasguillen.questlog.core.database.dao.SearchHistoryDao
import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.EngineEntity
import com.nikolasguillen.questlog.core.database.entity.GameArtworkEntity
import com.nikolasguillen.questlog.core.database.entity.GameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEngineCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEntity
import com.nikolasguillen.questlog.core.database.entity.GameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.GamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.OwnedPlatformEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.entity.RelatedGameEntity
import com.nikolasguillen.questlog.core.database.entity.SearchHistoryEntity
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity
import com.nikolasguillen.questlog.core.database.util.Converters

@Database(
    entities = [
        SearchHistoryEntity::class,
        GameEntity::class,
        ListEntity::class,
        GameListCrossRef::class,
        PlatformEntity::class,
        GamePlatformCrossRef::class,
        OwnedPlatformEntity::class,
        GenreEntity::class,
        GameGenreCrossRef::class,
        CompanyEntity::class,
        GameCompanyCrossRef::class,
        EngineEntity::class,
        GameEngineCrossRef::class,
        GameArtworkEntity::class,
        RelatedGameEntity::class,
        TranslatedDescriptionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QuestLogDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun listDao(): ListDao
    abstract fun platformDao(): PlatformDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun translationDao(): TranslationDao

    companion object {
        const val DATABASE_NAME = "quest_log_database"
    }
}
