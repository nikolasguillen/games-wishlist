package com.example.gameswishlist.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferences by preferencesDataStore(name = "user_preferences")

/**
 * Small app-wide toggles that are not tied to a game or a list, and therefore do not belong in Room —
 * unlike [com.example.gameswishlist.core.database.entity.OwnedPlatformEntity], a boolean has no rows to
 * be "empty" or "set", and this module's database wipes on every schema change while it stays
 * unpublished, which would silently reset a stored preference.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isDescriptionTranslationEnabled: Flow<Boolean> = context.userPreferences.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[DESCRIPTION_TRANSLATION_ENABLED_KEY] ?: false }

    suspend fun setDescriptionTranslationEnabled(enabled: Boolean) {
        context.userPreferences.edit { it[DESCRIPTION_TRANSLATION_ENABLED_KEY] = enabled }
    }

    private companion object {
        val DESCRIPTION_TRANSLATION_ENABLED_KEY = booleanPreferencesKey("description_translation_enabled")
    }
}
