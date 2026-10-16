package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupDialog
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/** The confirmation for [dialog]. `onConfirm` runs the action it guards, `onDismiss` closes it and does nothing. */
@Composable
internal fun AccountBackupDialogs(
    dialog: AccountBackupDialog,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    when (dialog) {
        AccountBackupDialog.ConfirmRestore -> CustomAlertDialog(
            title = stringResource(Res.string.account_backup_restore_title),
            message = stringResource(Res.string.account_backup_restore_message),
            confirmButtonText = stringResource(Res.string.account_backup_restore_confirm),
            onConfirm = onConfirm,
            dismissButtonText = stringResource(CoreUiRes.string.cancel),
            onDismiss = onDismiss
        )

        AccountBackupDialog.ConfirmEmptyBackup -> CustomAlertDialog(
            title = stringResource(Res.string.account_backup_empty_title),
            message = stringResource(Res.string.account_backup_empty_message),
            confirmButtonText = stringResource(Res.string.account_backup_empty_confirm),
            onConfirm = onConfirm,
            dismissButtonText = stringResource(CoreUiRes.string.cancel),
            onDismiss = onDismiss
        )

        AccountBackupDialog.ConfirmDeleteAccount -> CustomAlertDialog(
            title = stringResource(Res.string.account_backup_delete_title),
            message = stringResource(Res.string.account_backup_delete_message),
            confirmButtonText = stringResource(Res.string.account_backup_delete_confirm),
            onConfirm = onConfirm,
            dismissButtonText = stringResource(CoreUiRes.string.cancel),
            onDismiss = onDismiss
        )
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmRestoreDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmRestore,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmEmptyBackupDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmEmptyBackup,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmDeleteAccountDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmDeleteAccount,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
