package com.nikolasguillen.questlog.core.ui.component

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_action
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_message
import com.nikolasguillen.questlog.core.ui.resources.notification_permission_denied_title
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    CustomAlertDialog(
        title = stringResource(Res.string.notification_permission_denied_title),
        message = stringResource(Res.string.notification_permission_denied_message),
        confirmButtonText = stringResource(Res.string.notification_permission_denied_action),
        onConfirm = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
            onDismiss()
        },
        dismissButtonText = stringResource(Res.string.cancel),
        onDismiss = onDismiss
    )
}

@QuestLogPreviews
@Composable
private fun NotificationPermissionDeniedDialogPreview() {
    QuestLogTheme {
        NotificationPermissionDeniedDialog(onDismiss = {})
    }
}
