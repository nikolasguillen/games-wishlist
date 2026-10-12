package com.nikolasguillen.questlog.core.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * The notification permission as a cache that composition can observe. The system only answers through a callback,
 * so [status] holds the last answer: [refresh] updates it, and so does [request]. It starts as
 * [NotificationAuthorizationStatus.NOT_DETERMINED] until the first [refresh], which `startQuestLog` runs at launch.
 *
 * Process-wide, because the permission is: the Settings screens, the first-opt-in prompt and the reminder sync
 * triggers all read the same answer.
 */
object NotificationAuthorization {

    private val mutableStatus = MutableStateFlow(NotificationAuthorizationStatus.NOT_DETERMINED)

    val status: StateFlow<NotificationAuthorizationStatus> = mutableStatus.asStateFlow()

    /** Reads the permission from the system. Call it whenever the user may have changed it in Settings. */
    suspend fun refresh() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        mutableStatus.value = suspendCancellableCoroutine { continuation ->
            center.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(
                    when (settings?.authorizationStatus) {
                        UNAuthorizationStatusAuthorized,
                        UNAuthorizationStatusProvisional,
                        UNAuthorizationStatusEphemeral -> NotificationAuthorizationStatus.AUTHORIZED

                        UNAuthorizationStatusDenied -> NotificationAuthorizationStatus.DENIED
                        else -> NotificationAuthorizationStatus.NOT_DETERMINED
                    }
                )
            }
        }
    }

    /**
     * Shows the system prompt (it only appears while [status] is `NOT_DETERMINED`), refreshes [status] and returns
     * whether notifications were allowed.
     */
    suspend fun request(): Boolean {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val granted = suspendCancellableCoroutine { continuation ->
            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { allowed, _ ->
                continuation.resume(allowed)
            }
        }
        refresh()
        return granted
    }
}
