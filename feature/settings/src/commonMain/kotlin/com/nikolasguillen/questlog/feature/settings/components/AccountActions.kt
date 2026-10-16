package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_delete_account
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_sign_out
import org.jetbrains.compose.resources.stringResource

/**
 * Leaving the account: signing out keeps everything, deleting removes the account and its backup. Both are disabled
 * while any cloud operation runs ([runningOperation] set), and each shows a spinner while it is the one running.
 */
@Composable
internal fun AccountActions(
    runningOperation: CloudOperation?,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onSignOutClick,
            enabled = runningOperation == null,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            if (runningOperation == CloudOperation.SIGN_OUT) {
                CircularProgressIndicator(
                    modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                    strokeWidth = MaterialTheme.spacing.extraSmall
                )
            } else {
                Text(text = stringResource(Res.string.account_backup_sign_out))
            }
        }
        // The error colour is how the theme marks a destructive action: there is no dedicated component for it.
        TextButton(
            onClick = onDeleteAccountClick,
            enabled = runningOperation == null,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            if (runningOperation == CloudOperation.DELETE_ACCOUNT) {
                CircularProgressIndicator(
                    modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                    strokeWidth = MaterialTheme.spacing.extraSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(text = stringResource(Res.string.account_backup_delete_account))
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun AccountActionsPreview() {
    QuestLogTheme {
        AccountActions(runningOperation = null, onSignOutClick = {}, onDeleteAccountClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun AccountActionsDeletingPreview() {
    QuestLogTheme {
        AccountActions(
            runningOperation = CloudOperation.DELETE_ACCOUNT,
            onSignOutClick = {},
            onDeleteAccountClick = {}
        )
    }
}
