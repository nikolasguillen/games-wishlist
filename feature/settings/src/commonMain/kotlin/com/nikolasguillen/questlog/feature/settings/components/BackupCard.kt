package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomContentCard
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.retry
import com.nikolasguillen.questlog.feature.settings.model.BackupStatusUiModel
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_back_up_now
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_no_backup
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_restore
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_unreachable
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * What the cloud backup holds and the action that replaces it. Every action is disabled while any cloud operation
 * runs ([runningOperation] set), and "Back up now" shows its own spinner while it is the one running.
 */
@Composable
internal fun BackupCard(
    backup: BackupStatusUiModel,
    runningOperation: CloudOperation?,
    onBackUpClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomContentCard(modifier = modifier.fillMaxWidth()) {
        Column {
            when (backup) {
                is BackupStatusUiModel.Available -> {
                    Text(
                        text = backup.lastBackup.asString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = backup.summary.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BackupStatusUiModel.None -> Text(
                    text = stringResource(Res.string.account_backup_no_backup),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                BackupStatusUiModel.Unknown -> {
                    Text(
                        text = stringResource(Res.string.account_backup_unreachable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.medium))
                    OutlinedButton(
                        onClick = onRetryClick,
                        enabled = runningOperation == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(CoreUiRes.string.retry))
                    }
                }
            }

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.medium))

            ProviderSignInButton(
                label = stringResource(Res.string.account_backup_back_up_now),
                loading = runningOperation == CloudOperation.BACK_UP,
                enabled = runningOperation == null,
                onClick = onBackUpClick
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.medium))
            // There is nothing to restore until a backup exists, and it is read from the server, so an unreachable one
            // cannot be restored either.
            val restoreLabel = stringResource(Res.string.account_backup_restore)
            OutlinedButton(
                onClick = onRestoreClick,
                enabled = runningOperation == null && backup is BackupStatusUiModel.Available,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = restoreLabel }
            ) {
                if (runningOperation == CloudOperation.RESTORE) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(MaterialTheme.spacing.extraLarge),
                        strokeWidth = MaterialTheme.spacing.extraSmall
                    )
                } else {
                    Text(text = restoreLabel)
                }
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun BackupCardAvailablePreview() {
    QuestLogTheme {
        BackupCard(
            backup = BackupStatusUiModel.Available(
                lastBackup = UiText.DynamicString("Last backup: Mar 15, 2024, 8:00 AM"),
                summary = UiText.DynamicString("3 lists, 20 games")
            ),
            runningOperation = null,
            onBackUpClick = {},
            onRestoreClick = {},
            onRetryClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun BackupCardNonePreview() {
    QuestLogTheme {
        BackupCard(
            BackupStatusUiModel.None,
            runningOperation = null,
            onBackUpClick = {},
            onRestoreClick = {},
            onRetryClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun BackupCardBusyPreview() {
    QuestLogTheme {
        BackupCard(
            BackupStatusUiModel.None,
            runningOperation = CloudOperation.BACK_UP,
            onBackUpClick = {},
            onRestoreClick = {},
            onRetryClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun BackupCardUnknownPreview() {
    QuestLogTheme {
        BackupCard(
            BackupStatusUiModel.Unknown,
            runningOperation = null,
            onBackUpClick = {},
            onRestoreClick = {},
            onRetryClick = {}
        )
    }
}
