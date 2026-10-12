package com.nikolasguillen.questlog.core.domain.notification

/**
 * Posts release-day reminders. Contract only: `:core:domain` never imports the Android notification APIs.
 * Only a platform whose reminders run app code at fire time calls it; iOS lets the system deliver its local
 * notifications, so nothing there implements or calls it.
 */
interface ReleaseNotifier {
    /** Posts the reminder for [gameId], titled with [gameName]. Tapping it opens that game's detail screen. */
    suspend fun notifyReleased(gameId: Int, gameName: String)
}
