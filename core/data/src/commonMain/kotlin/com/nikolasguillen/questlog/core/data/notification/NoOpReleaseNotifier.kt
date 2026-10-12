package com.nikolasguillen.questlog.core.data.notification

import com.nikolasguillen.questlog.core.domain.notification.ReleaseNotifier

/**
 * [ReleaseNotifier] for a platform that never posts a reminder itself: it posts nothing.
 */
internal class NoOpReleaseNotifier : ReleaseNotifier {
    override suspend fun notifyReleased(gameId: Int, gameName: String) = Unit
}
