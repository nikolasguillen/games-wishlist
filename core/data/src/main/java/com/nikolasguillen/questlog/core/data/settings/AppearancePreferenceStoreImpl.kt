package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikolasguillen.questlog.core.domain.settings.AppearancePreferenceStore
import com.nikolasguillen.questlog.core.model.AppearanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val APPEARANCE_MODE_KEY = intPreferencesKey("appearance_mode")

/**
 * The `DataStore<Preferences>` instance itself is provided by [com.nikolasguillen.questlog.core.data.di.DataModule]
 * rather than built here, so this class stays a plain function of an injected [DataStore] — directly
 * unit-testable with a temp-file-backed instance, no Android `Context`/Robolectric required.
 */
class AppearancePreferenceStoreImpl(
    private val settingsDataStore: DataStore<Preferences>
) : AppearancePreferenceStore {

    override fun observeAppearanceMode(): Flow<AppearanceMode> =
        settingsDataStore.data.map { prefs ->
            AppearanceMode.fromId(prefs[APPEARANCE_MODE_KEY] ?: AppearanceMode.SYSTEM.id)
        }

    override suspend fun setAppearanceMode(mode: AppearanceMode) {
        settingsDataStore.edit { prefs -> prefs[APPEARANCE_MODE_KEY] = mode.id }
    }
}
