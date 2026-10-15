package com.nikolasguillen.questlog.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.AuthProvider
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.back_content_description
import com.nikolasguillen.questlog.feature.settings.components.AccountBackupDialogs
import com.nikolasguillen.questlog.feature.settings.components.AccountCard
import com.nikolasguillen.questlog.feature.settings.components.BackupCard
import com.nikolasguillen.questlog.feature.settings.components.ProviderSignInButton
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupDialog
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEffect
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiEvent
import com.nikolasguillen.questlog.feature.settings.model.AccountBackupUiState
import com.nikolasguillen.questlog.feature.settings.model.AccountUiModel
import com.nikolasguillen.questlog.feature.settings.model.BackupStatusUiModel
import com.nikolasguillen.questlog.feature.settings.model.CloudOperation
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_continue_with_google
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_explainer
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_sign_in_with_apple
import com.nikolasguillen.questlog.feature.settings.resources.account_backup_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun AccountBackupScreen(
    viewModel: AccountBackupViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is AccountBackupUiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.text.resolve())
                }
            }
        }
    }

    AccountBackupContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * Signed out, only the explainer and the two sign-in buttons are offered: the app works fully without an account, and
 * nothing is stored in the cloud until the user signs in and asks for a backup. Signed in, the account and the backup
 * card take their place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountBackupContent(
    state: AccountBackupUiState,
    onEvent: (AccountBackupUiEvent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.account_backup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiRes.string.back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
        ) {
            val account = state.account
            if (account == null) {
                SignedOutContent(state = state, onEvent = onEvent)
            } else {
                AccountCard(account = account)
                BackupCard(
                    backup = state.backup,
                    runningOperation = state.runningOperation,
                    onBackUpClick = { onEvent(AccountBackupUiEvent.BackUp) },
                    onRestoreClick = { onEvent(AccountBackupUiEvent.Restore) },
                    onRetryClick = { onEvent(AccountBackupUiEvent.Retry) }
                )
            }
        }
    }

    state.dialog?.let { dialog ->
        AccountBackupDialogs(
            dialog = dialog,
            onConfirm = {
                when (dialog) {
                    AccountBackupDialog.ConfirmEmptyBackup -> onEvent(AccountBackupUiEvent.ConfirmEmptyBackup)
                    AccountBackupDialog.ConfirmRestore -> onEvent(AccountBackupUiEvent.ConfirmRestore)
                }
            },
            onDismiss = { onEvent(AccountBackupUiEvent.DismissDialog) }
        )
    }
}

@Composable
private fun SignedOutContent(state: AccountBackupUiState, onEvent: (AccountBackupUiEvent) -> Unit) {
    val busy = state.runningOperation != null
    Text(
        text = stringResource(Res.string.account_backup_explainer),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ProviderSignInButton(
        label = stringResource(
            when (state.signInProvider) {
                AuthProvider.GOOGLE -> Res.string.account_backup_continue_with_google
                AuthProvider.APPLE -> Res.string.account_backup_sign_in_with_apple
            }
        ),
        loading = state.runningOperation == CloudOperation.SIGN_IN,
        enabled = !busy,
        onClick = { onEvent(AccountBackupUiEvent.SignIn) }
    )
}

@QuestLogPreviews
@Composable
private fun AccountBackupSignedOutPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupSignedOutAppleiOSPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(signInProvider = AuthProvider.APPLE),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupSignedInNoBackupPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(
                account = AccountUiModel("ada@example.com", listOf(AuthProvider.GOOGLE)),
                backup = BackupStatusUiModel.None
            ),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupSignedInWithBackupPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(
                account = AccountUiModel("ada@example.com", listOf(AuthProvider.GOOGLE)),
                backup = BackupStatusUiModel.Available(
                    lastBackup = UiText.DynamicString("Last backup: Mar 15, 2024, 8:00 AM"),
                    summary = UiText.DynamicString("3 lists, 20 games")
                )
            ),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupBusyPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(
                account = AccountUiModel("ada@example.com", listOf(AuthProvider.GOOGLE)),
                backup = BackupStatusUiModel.None,
                runningOperation = CloudOperation.BACK_UP
            ),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupRestoreDialogPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(
                account = AccountUiModel("ada@example.com", listOf(AuthProvider.GOOGLE)),
                backup = BackupStatusUiModel.Available(
                    lastBackup = UiText.DynamicString("Last backup: Mar 15, 2024, 8:00 AM"),
                    summary = UiText.DynamicString("3 lists, 20 games")
                ),
                dialog = AccountBackupDialog.ConfirmRestore
            ),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun AccountBackupEmptyDialogPreview() {
    QuestLogTheme {
        AccountBackupContent(
            state = AccountBackupUiState(
                account = AccountUiModel("ada@example.com", listOf(AuthProvider.GOOGLE)),
                backup = BackupStatusUiModel.None,
                dialog = AccountBackupDialog.ConfirmEmptyBackup
            ),
            onEvent = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
