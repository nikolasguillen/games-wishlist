package com.example.gameswishlist.core.domain.radar

/**
 * Schedules the periodic saved-game release-date refresh. Contract only: `:core:domain` never imports
 * `androidx.work` — the WorkManager-backed implementation lives in `:core:data`, per the KMP-reversibility
 * note in the roadmap (WorkManager has no multiplatform counterpart).
 */
interface ReleaseRefreshScheduler {
    fun schedulePeriodicRefresh()

    /** One-shot refresh for when the saved set just changed, so a new game doesn't wait for the 24h cycle. */
    fun scheduleImmediateRefresh()
}
