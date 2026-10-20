package com.nikolasguillen.questlog.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity

@Dao
interface TranslationDao {
    @Query(
        "SELECT * FROM translated_descriptions " +
            "WHERE subjectType = :subjectType AND subjectId = :subjectId AND languageTag = :languageTag"
    )
    suspend fun getTranslation(subjectType: String, subjectId: Int, languageTag: String): TranslatedDescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTranslation(entity: TranslatedDescriptionEntity)
}
