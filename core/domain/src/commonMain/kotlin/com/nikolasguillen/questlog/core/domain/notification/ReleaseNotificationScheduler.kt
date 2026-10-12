package com.nikolasguillen.questlog.core.domain.notification

/**
 * Schedules per-game release-day reminders. Contract only: `:core:domain` never imports a platform scheduler.
 * Each platform has its own implementation in `:core:data`, the same shape as
 * [com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler].
 */
interface ReleaseNotificationScheduler {
    /** Arms (or re-arms) the reminder for [reminder].gameId, replacing any pending one. */
    suspend fun schedule(reminder: ReleaseReminder)

    /** Removes any pending reminder for [gameId]. A no-op when none is scheduled. */
    suspend fun cancel(gameId: Int)

    /**
     * Reminders the system delivered without running app code since the last call, as gameId to the release date
     * each was armed for. Each one is reported once. A platform whose worker records delivery itself returns an
     * empty map.
     */
    suspend fun takeFiredReminders(): Map<Int, Long>
}
