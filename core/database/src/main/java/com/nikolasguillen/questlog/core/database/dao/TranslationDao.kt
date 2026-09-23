package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translated_descriptions WHERE gameId = :gameId AND languageTag = :languageTag")
    suspend fun getTranslation(gameId: Int, languageTag: String): TranslatedDescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTranslation(entity: TranslatedDescriptionEntity)
}
