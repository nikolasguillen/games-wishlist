package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.got_it
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupDialog
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_deleting_account_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_empty_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_sign_out_confirm
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_sign_out_message
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_sign_out_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signing_in_title
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_signing_out_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * The dialog for [dialog]. For a confirmation `onConfirm` runs the action it guards and `onDismiss` closes it and does
 * nothing. A sign-in, sign-out or account deletion has no action to confirm: while it runs nothing can close it, and once it ends its one
 * button, "Got it", is `onDismiss`.
 */
@Composable
internal fun AccountBackupDialogs(
    dialog: AccountBackupDialog, onConfirm: () -> Unit, onDismiss: () -> Unit
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

        AccountBackupDialog.ConfirmSignOut -> CustomAlertDialog(
            title = stringResource(Res.string.account_backup_sign_out_title),
            message = stringResource(Res.string.account_backup_sign_out_message),
            confirmButtonText = stringResource(Res.string.account_backup_sign_out_confirm),
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

        is AccountBackupDialog.AccountChange -> AccountChangeDialog(
            dialog = dialog, onDismiss = onDismiss
        )
    }
}

@Composable
private fun AccountChangeDialog(dialog: AccountBackupDialog.AccountChange, onDismiss: () -> Unit) {
    val outcome = dialog.outcome
    CustomAlertDialog(
        title = stringResource(
            when (dialog.operation) {
                CloudOperation.SIGN_IN -> Res.string.account_backup_signing_in_title
                CloudOperation.DELETE_ACCOUNT -> Res.string.account_backup_deleting_account_title
                else -> Res.string.account_backup_signing_out_title
            }
        ),
        confirmButtonText = if (outcome == null) "" else stringResource(CoreUiRes.string.got_it),
        onConfirm = onDismiss,
        // Back and a tap outside ask to dismiss too; while it runs they are ignored.
        onDismiss = if (outcome == null) ({}) else onDismiss
    ) {
        if (outcome == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearWavyProgressIndicator(
                    trackColor = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Text(text = outcome.asString())
        }
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmRestoreDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmRestore,
            onConfirm = {},
            onDismiss = {})
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmEmptyBackupDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmEmptyBackup,
            onConfirm = {},
            onDismiss = {})
    }
}

@QuestLogPreviews
@Composable
private fun ConfirmSignOutDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.ConfirmSignOut,
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
            onDismiss = {})
    }
}

@QuestLogPreviews
@Composable
private fun SigningInDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.AccountChange(CloudOperation.SIGN_IN),
            onConfirm = {},
            onDismiss = {})
    }
}

@QuestLogPreviews
@Composable
private fun DeletingAccountDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.AccountChange(CloudOperation.DELETE_ACCOUNT),
            onConfirm = {},
            onDismiss = {})
    }
}

@QuestLogPreviews
@Composable
private fun SignedOutDialogPreview() {
    QuestLogTheme {
        AccountBackupDialogs(
            dialog = AccountBackupDialog.AccountChange(
                operation = CloudOperation.SIGN_OUT,
                outcome = UiText.DynamicString("You are signed out.")
            ), onConfirm = {}, onDismiss = {})
    }
}
