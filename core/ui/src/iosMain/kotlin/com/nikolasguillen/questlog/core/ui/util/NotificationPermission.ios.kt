package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable

/**
 * Never read on iOS, because every call site is behind `ReleaseRemindersAvailability.isAvailable`. It reports "not
 * granted" and does nothing when asked, so that a call that did slip through could not post anything.
 */
@Composable
actual fun rememberNotificationPermissionState(
    onResult: (granted: Boolean) -> Unit
): NotificationPermissionState = NotificationPermissionState(
    canDeliver = false,
    isPermanentlyDenied = false,
    requiresRuntimePermission = true,
    request = {}
)
