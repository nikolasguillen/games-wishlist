package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_action
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_message
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_title
import com.nikolasguillen.questlog.core.ui.util.openAppNotificationSettings
import org.jetbrains.compose.resources.stringResource

/** Explains that notifications are off and sends the user to the app's notification settings. */
@Composable
actual fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(Res.string.notification_permission_denied_title),
        message = stringResource(Res.string.notification_permission_denied_message),
        confirmButtonText = stringResource(Res.string.notification_permission_denied_action),
        onConfirm = {
            openAppNotificationSettings()
            onDismiss()
        },
        dismissButtonText = stringResource(Res.string.cancel),
        onDismiss = onDismiss
    )
}
