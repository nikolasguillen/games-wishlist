package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val WISHLIST_VIEW_MODE_KEY = intPreferencesKey("wishlist_view_mode")

/**
 * Shares the `"settings"` [DataStore] with [AppearancePreferenceStoreImpl]; it is provided by
 * [com.nikolasguillen.questlog.core.data.di.DataModule] so this class stays a plain function of an injected
 * [DataStore], directly unit-testable with a temp-file-backed instance.
 */
class WishlistViewModePreferenceStoreImpl(
    private val settingsDataStore: DataStore<Preferences>
) : WishlistViewModePreferenceStore {

    override fun observeWishlistViewMode(): Flow<WishlistViewMode> =
        settingsDataStore.data.map { prefs ->
            WishlistViewMode.fromId(prefs[WISHLIST_VIEW_MODE_KEY] ?: WishlistViewMode.LIST.id)
        }

    override suspend fun setWishlistViewMode(mode: WishlistViewMode) {
        settingsDataStore.edit { prefs -> prefs[WISHLIST_VIEW_MODE_KEY] = mode.id }
    }
}
