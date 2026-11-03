package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalWindowInfo
import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.common.desktop.isMacOs
import com.nikolasguillen.questlog.core.common.desktop.openSystemNotificationSettings
import kotlinx.coroutines.launch

/**
 * Reads the [DesktopNotificationAuthorization] cache. macOS shows its prompt once: while the permission is undecided,
 * `request` shows it, and after that the permission can only be changed in System Settings, so `request` opens them.
 * Windows has no prompt. Notifications that are off can only be turned on in Windows Settings, so a request there
 * reports a refusal at once and the state says "permanently denied": the opt-in helper then shows the denied dialog,
 * whose button opens the settings.
 */
@Composable
actual fun rememberNotificationPermissionState(
    onResult: (granted: Boolean) -> Unit
): NotificationPermissionState {
    val status by DesktopNotificationAuthorization.status.collectAsState()
    val latestOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()
    val isWindows = !isMacOs()
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused

    // The user may have changed the permission in the OS settings and come back: that is the window regaining focus.
    LaunchedEffect(isWindowFocused) {
        if (isWindowFocused) DesktopNotificationAuthorization.refresh()
    }

    return status.toPermissionState(isWindows) {
        when {
            isWindows -> latestOnResult(false)

            status == DesktopNotificationStatus.NOT_DETERMINED ->
                scope.launch { latestOnResult(DesktopNotificationAuthorization.request()) }

            else -> openSystemNotificationSettings()
        }
    }
}
