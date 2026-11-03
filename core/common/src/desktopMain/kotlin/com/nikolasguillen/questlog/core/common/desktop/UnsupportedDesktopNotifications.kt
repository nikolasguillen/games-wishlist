package com.nikolasguillen.questlog.core.common.desktop

/** The notifications of an OS without an implementation: every call reports that nothing can be delivered. */
internal object UnsupportedDesktopNotifications : DesktopNotifications {
    override suspend fun status() = DesktopNotificationStatus.UNSUPPORTED

    override suspend fun requestAuthorization() = false

    override suspend fun schedule(request: DesktopNotificationRequest) = false

    override suspend fun pending() = emptyList<DesktopPendingNotification>()

    override suspend fun remove(gameIds: Collection<Int>) = Unit

    override fun setClickHandler(onClick: (gameId: Int) -> Unit) = Unit
}
