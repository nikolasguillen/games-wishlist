package com.nikolasguillen.questlog.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

/**
 * The `DataStore<Preferences>` instance itself is provided by [com.nikolasguillen.questlog.core.data.di.DataModule]
 * rather than built here, so this class stays a plain function of an injected [DataStore] — directly
 * unit-testable with a temp-file-backed instance, no Android `Context`/Robolectric required.
 *
 * The flag lives outside Room on purpose: the database is destructively migrated while the app is
 * unpublished, and a wiped schema must not show the welcome flow again.
 */
class OnboardingPreferenceStoreImpl @Inject constructor(
    private val settingsDataStore: DataStore<Preferences>
) : OnboardingPreferenceStore {

    override fun observeOnboardingCompleted(): Flow<Boolean> =
        settingsDataStore.data.map { prefs -> prefs[ONBOARDING_COMPLETED_KEY] ?: false }

    override suspend fun setOnboardingCompleted() {
        settingsDataStore.edit { prefs -> prefs[ONBOARDING_COMPLETED_KEY] = true }
    }
}
