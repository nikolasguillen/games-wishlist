package com.nikolasguillen.questlog.core.ui.util

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.desktopNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The notification permission as a cache that composition can observe, the desktop counterpart of iOS's
 * `NotificationAuthorization`. The system answers through suspending calls, so [status] holds the last answer:
 * [refresh] updates it, and so does [request]. It starts as [DesktopNotificationStatus.NOT_DETERMINED] until the first
 * [refresh], which the app runs at launch.
 *
 * Process-wide, because the permission is: Settings, the first-opt-in prompt and the reminder sync triggers all read
 * the same answer. Public, because `:shared` observes it.
 */
object DesktopNotificationAuthorization {

    private val mutableStatus = MutableStateFlow(DesktopNotificationStatus.NOT_DETERMINED)

    val status: StateFlow<DesktopNotificationStatus> = mutableStatus.asStateFlow()

    /** Reads the permission from the system. Call it whenever the user may have changed it in the OS settings. */
    suspend fun refresh() {
        mutableStatus.value = desktopNotifications().status()
    }

    /** Shows the macOS prompt (nothing appears on Windows), refreshes [status] and returns whether it is allowed. */
    suspend fun request(): Boolean {
        val granted = desktopNotifications().requestAuthorization()
        refresh()
        return granted
    }
}
