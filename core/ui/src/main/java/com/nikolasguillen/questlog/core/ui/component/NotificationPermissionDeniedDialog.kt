package com.nikolasguillen.questlog.core.ui.component

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.R

/**
 * Shown right after the user opts in to a release reminder but the system notification permission is
 * denied, so the opt-in is never left silently "on" with no way to ever notify (FR-012).
 */
@Composable
fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    CustomAlertDialog(
        title = stringResource(R.string.notification_permission_denied_title),
        message = stringResource(R.string.notification_permission_denied_message),
        confirmButtonText = stringResource(R.string.notification_permission_denied_action),
        onConfirm = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
            onDismiss()
        },
        dismissButtonText = stringResource(R.string.cancel),
        onDismiss = onDismiss
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionDeniedDialogPreview() {
    QuestLogTheme {
        NotificationPermissionDeniedDialog(onDismiss = {})
    }
}
