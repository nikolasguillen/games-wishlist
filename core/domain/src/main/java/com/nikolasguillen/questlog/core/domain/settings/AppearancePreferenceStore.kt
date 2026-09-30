package com.nikolasguillen.questlog.core.domain.settings

import com.nikolasguillen.questlog.core.model.AppearanceMode
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the user's [AppearanceMode] choice. Contract only: `:core:domain` never imports
 * `androidx.datastore`.
 */
interface AppearancePreferenceStore {
    /**
     * Emits the current [AppearanceMode] and every subsequent change. Defaults to [AppearanceMode.SYSTEM]
     * when nothing has been persisted yet.
     */
    fun observeAppearanceMode(): Flow<AppearanceMode>

    /** Persists [mode] as the new selection. Overwrites any previous value. */
    suspend fun setAppearanceMode(mode: AppearanceMode)
}
