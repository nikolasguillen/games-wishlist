package com.nikolasguillen.questlog.core.domain.notification

import kotlin.time.Instant

/**
 * Schedules per-game release-day reminders. Contract only: `:core:domain` never imports `androidx.work` —
 * the WorkManager-backed implementation lives in `:core:data`, per the KMP-reversibility note in the
 * roadmap, the same shape as [com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler].
 */
interface ReleaseNotificationScheduler {
    /** Enqueues (or re-enqueues) the reminder for [gameId] to fire at [at]. Replaces any existing one. */
    fun schedule(gameId: Int, at: Instant)

    /** Cancels any pending reminder for [gameId]. A no-op when none is scheduled. */
    fun cancel(gameId: Int)
}
