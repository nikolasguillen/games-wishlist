package com.nikolasguillen.questlog.core.domain.settings

import com.nikolasguillen.questlog.core.model.WishlistViewMode
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the user's [WishlistViewMode] choice. One app-wide value, shared by every wishlist.
 * Contract only: `:core:domain` never imports `androidx.datastore`.
 */
interface WishlistViewModePreferenceStore {
    /**
     * Emits the current [WishlistViewMode] and every subsequent change. Defaults to [WishlistViewMode.LIST]
     * when nothing has been persisted yet.
     */
    fun observeWishlistViewMode(): Flow<WishlistViewMode>

    /** Persists [mode] as the new selection. Overwrites any previous value. */
    suspend fun setWishlistViewMode(mode: WishlistViewMode)
}
