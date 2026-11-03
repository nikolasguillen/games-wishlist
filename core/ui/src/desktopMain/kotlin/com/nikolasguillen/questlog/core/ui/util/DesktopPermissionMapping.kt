package com.nikolasguillen.questlog.core.ui.util

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus

/**
 * The permission state a screen sees for the OS [this] status, as the table in data-model §2 lists it.
 *
 * Windows has no prompt, so "not enabled" is the only blocked state and it is permanent from the app's point of view:
 * the user has to turn notifications on in Windows Settings. macOS asks once, while the status is
 * `NOT_DETERMINED`, and an app without a bundle (`UNSUPPORTED`) cannot be allowed at all.
 */
internal fun DesktopNotificationStatus.toPermissionState(
    isWindows: Boolean,
    request: () -> Unit
): NotificationPermissionState {
    val canDeliver = this == DesktopNotificationStatus.AUTHORIZED
    return NotificationPermissionState(
        canDeliver = canDeliver,
        isPermanentlyDenied = if (isWindows) {
            !canDeliver
        } else {
            this == DesktopNotificationStatus.DENIED || this == DesktopNotificationStatus.UNSUPPORTED
        },
        requiresRuntimePermission = !isWindows,
        request = request
    )
}
