package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification

/**
 * Reads the [NotificationAuthorization] cache. iOS shows its prompt once: while the permission is undecided,
 * `request` shows it, and after that it can only be changed in Settings, so `request` opens the app's notification
 * settings. That makes "denied" the only denied state, hence [NotificationPermissionState.isPermanentlyDenied].
 */
@Composable
actual fun rememberNotificationPermissionState(
    onResult: (granted: Boolean) -> Unit
): NotificationPermissionState {
    val status by NotificationAuthorization.status.collectAsState()
    val latestOnResult by rememberUpdatedState(onResult)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { NotificationAuthorization.refresh() }

    // The user may have changed the permission in Settings and come back: that is the app becoming active again.
    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { scope.launch { NotificationAuthorization.refresh() } }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    return NotificationPermissionState(
        canDeliver = status == NotificationAuthorizationStatus.AUTHORIZED,
        isPermanentlyDenied = status == NotificationAuthorizationStatus.DENIED,
        requiresRuntimePermission = true,
        request = {
            if (status == NotificationAuthorizationStatus.NOT_DETERMINED) {
                scope.launch { latestOnResult(NotificationAuthorization.request()) }
            } else {
                openAppNotificationSettings()
            }
        }
    )
}
