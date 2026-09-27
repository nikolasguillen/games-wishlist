package com.nikolasguillen.questlog.core.domain.notification

/**
 * Posts release-day reminders. Contract only: `:core:domain` never imports the Android notification APIs.
 */
interface ReleaseNotifier {
    /** Posts the reminder for [gameId], titled with [gameName]. Tapping it opens that game's detail screen. */
    fun notifyReleased(gameId: Int, gameName: String)

    /** True when the app can currently deliver a notification at all (permission granted and not disabled). */
    fun canDeliver(): Boolean
}
