package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Stub so the module compiles while the desktop reminders are built (specs/020-desktop-app, T023). Replaced in T078.
@Composable
actual fun rememberNotificationPermissionState(onResult: (granted: Boolean) -> Unit): NotificationPermissionState =
    remember {
        NotificationPermissionState(
            canDeliver = false,
            isPermanentlyDenied = false,
            requiresRuntimePermission = false,
            request = {}
        )
    }
