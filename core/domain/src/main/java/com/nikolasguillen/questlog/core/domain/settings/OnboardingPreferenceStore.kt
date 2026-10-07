package com.nikolasguillen.questlog.core.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes whether the user has been through the welcome flow. Contract only: `:core:domain`
 * never imports `androidx.datastore`.
 */
interface OnboardingPreferenceStore {
    /**
     * Emits whether the welcome flow was completed or skipped, and every subsequent change. `false`
     * until [setOnboardingCompleted] is first called.
     */
    fun observeOnboardingCompleted(): Flow<Boolean>

    /** Marks the welcome flow as completed. Idempotent: calling it again changes nothing. */
    suspend fun setOnboardingCompleted()
}
