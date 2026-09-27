package com.nikolasguillen.questlog.core.ui.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * The system notification permission's current, composition-observable state, shared by every surface
 * that offers a release-notification opt-in.
 *
 * @property canDeliver True when a notification posted right now would actually reach the user —
 * [NotificationManagerCompat.areNotificationsEnabled], not a runtime-permission check: below API 33 there
 * is no runtime permission at all, but the user can still have disabled notifications in system settings.
 * @property isPermanentlyDenied True once the system will no longer show its own request dialog, so only
 * the app's notification settings screen can fix it.
 * @property request Requests the permission on API 33+ (or opens the app's notification settings directly
 * once [isPermanentlyDenied], or on any API below 33 where there is nothing to request).
 */
@Immutable
data class NotificationPermissionState(
    val canDeliver: Boolean,
    val isPermanentlyDenied: Boolean,
    val request: () -> Unit
)

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current

    var canDeliver by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        canDeliver = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val activity = context.findActivity()
        if (!granted && activity != null) {
            isPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDeliver = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return NotificationPermissionState(
        canDeliver = canDeliver,
        isPermanentlyDenied = isPermanentlyDenied,
        request = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPermanentlyDenied) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
