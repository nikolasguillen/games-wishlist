package com.nikolasguillen.questlog.core.common.desktop

/**
 * The operating system's notification centre as the desktop app uses it: Windows scheduled toasts or macOS
 * `UserNotifications`. Nothing here throws except `CancellationException`: a failure is `false`, an empty list or
 * [DesktopNotificationStatus.UNSUPPORTED]. Every call runs off the main thread.
 */
interface DesktopNotifications {
    suspend fun status(): DesktopNotificationStatus

    /** Shows the macOS prompt. Windows has none, so it only reports whether notifications are enabled. */
    suspend fun requestAuthorization(): Boolean

    /** `false` when notifications are not authorized or the system rejected the request. */
    suspend fun schedule(request: DesktopNotificationRequest): Boolean

    suspend fun pending(): List<DesktopPendingNotification>

    suspend fun remove(gameIds: Collection<Int>)

    /** Reports a click on one of our notifications, on any thread. Called once, at the start of `main`. */
    fun setClickHandler(onClick: (gameId: Int) -> Unit)
}
