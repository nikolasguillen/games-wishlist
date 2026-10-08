package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * The system notification permission's current, composition-observable state, shared by every surface
 * that offers a release-notification opt-in.
 *
 * @property canDeliver True when a notification posted right now would actually reach the user. On Android
 * that is `NotificationManagerCompat.areNotificationsEnabled`, not a runtime-permission check: below API 33
 * there is no runtime permission at all, but the user can still have disabled notifications in system settings.
 * @property isPermanentlyDenied True once the system will no longer show its own request dialog, so only
 * the app's notification settings screen can fix it.
 * @property requiresRuntimePermission Whether the system asks the user for a runtime permission at all. Android
 * below API 33 has none: notifications are on unless the user turned them off in settings.
 * @property request Requests the permission (or opens the app's notification settings directly once
 * [isPermanentlyDenied], or where there is nothing to request).
 */
@Immutable
data class NotificationPermissionState(
    val canDeliver: Boolean,
    val isPermanentlyDenied: Boolean,
    val requiresRuntimePermission: Boolean,
    val request: () -> Unit
)

/**
 * @param onResult Called with the outcome each time the system permission request returns, after
 * [NotificationPermissionState.canDeliver] and [NotificationPermissionState.isPermanentlyDenied] have been
 * updated. It is the only reliable signal of a first denial: neither of those changes then, so observing
 * the state cannot tell "denied" from "not asked yet".
 */
@Composable
expect fun rememberNotificationPermissionState(
    onResult: (granted: Boolean) -> Unit = {}
): NotificationPermissionState
