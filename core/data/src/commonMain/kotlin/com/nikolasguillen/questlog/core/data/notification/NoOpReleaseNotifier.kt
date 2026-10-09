package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier

/**
 * [ReleaseNotifier] for a platform without release reminders: it posts nothing and reports that it could not.
 */
internal class NoOpReleaseNotifier : ReleaseNotifier {
    override fun notifyReleased(gameId: Int, gameName: String) = Unit

    override fun canDeliver(): Boolean = false
}
