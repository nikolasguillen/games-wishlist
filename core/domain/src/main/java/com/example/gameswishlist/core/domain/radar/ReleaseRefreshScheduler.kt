package com.example.gameswishlist.core.domain.radar

/**
 * Schedules the periodic saved-game release-date refresh. Contract only: `:core:domain` never imports
 * `androidx.work` — the WorkManager-backed implementation lives in `:core:data`, per the KMP-reversibility
 * note in the roadmap (WorkManager has no multiplatform counterpart).
 */
interface ReleaseRefreshScheduler {
    fun schedulePeriodicRefresh()
}
