package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.nikolasguillen.questlog.core.ui.component.NotificationPermissionDeniedDialog

/**
 * The permission half of a release-reminder opt-in, shared by every surface that offers one. It returns the
 * function to call when the view model asks for the notification permission, and it shows
 * [NotificationPermissionDeniedDialog] itself when that request ends with notifications still off.
 *
 * The returned function does nothing once notifications can already be delivered, and it is the same instance
 * on every recomposition, so an effect that never restarts can hold on to it. The dialog reacts only to the
 * outcome of a request made through it, never to notifications merely being off when the screen opens.
 */
@Composable
fun rememberNotificationOptInRequest(): () -> Unit {
    val permissionState = rememberNotificationPermissionState()
    // The function returned below outlives any one composition, so a plain `permissionState` capture would freeze
    // at whatever canDeliver/isPermanentlyDenied were on first composition -- e.g. staying "permanently denied"
    // forever even after the user grants the permission from system Settings and comes back.
    // rememberUpdatedState keeps the reference it closes over live: reading `.value` always sees the latest state.
    val latestPermissionState = rememberUpdatedState(permissionState)
    var showDeniedDialog by rememberSaveable { mutableStateOf(false) }
    // Set only while a request is in flight, so the dialog reacts to that specific request's outcome instead of
    // nagging on every screen visit while notifications happen to be off.
    var awaitingResult by remember { mutableStateOf(false) }

    LaunchedEffect(permissionState.canDeliver, permissionState.isPermanentlyDenied) {
        if (awaitingResult && !permissionState.canDeliver) showDeniedDialog = true
        awaitingResult = false
    }

    if (showDeniedDialog) {
        NotificationPermissionDeniedDialog(onDismiss = { showDeniedDialog = false })
    }

    return remember {
        {
            if (!latestPermissionState.value.canDeliver) {
                awaitingResult = true
                latestPermissionState.value.request()
            }
        }
    }
}
