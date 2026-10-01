package com.nikolasguillen.questlog.feature.gamedetail

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.core.ui.component.NotificationPermissionDeniedDialog
import com.nikolasguillen.questlog.core.ui.util.rememberNotificationPermissionState
import com.nikolasguillen.questlog.feature.gamedetail.components.GameDetailMainContent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailContentState
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEffect
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEvent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiState
import kotlinx.coroutines.launch

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permissionState = rememberNotificationPermissionState()
    // LaunchedEffect(viewModel, lifecycle) below never restarts once launched (viewModel/lifecycle are
    // stable for the screen's whole life), so a plain `permissionState` capture would freeze it at
    // whatever canDeliver/isPermanentlyDenied were on first composition -- e.g. staying "permanently
    // denied" forever even after the user grants the permission from system Settings and comes back.
    // rememberUpdatedState keeps the reference the effect closes over live: reading `.value` inside the
    // coroutine always sees the latest permission state instead of the one captured at launch.
    val latestPermissionState = rememberUpdatedState(permissionState)
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    // Set only while an opt-in-triggered request is in flight, so the dialog reacts to that specific
    // request's outcome instead of nagging on every screen visit while notifications happen to be off.
    var awaitingPermissionResult by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is GameDetailUiEffect.ShareGame -> {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.text.asString(context))
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }

                    is GameDetailUiEffect.NavigateToGame -> {
                        onGameClick(effect.id)
                    }

                    GameDetailUiEffect.RequestNotificationPermission -> {
                        if (!latestPermissionState.value.canDeliver) {
                            awaitingPermissionResult = true
                            latestPermissionState.value.request()
                        }
                    }

                    is GameDetailUiEffect.ShowSnackbar -> {
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(effect.message.asString(context))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(permissionState.canDeliver, permissionState.isPermanentlyDenied) {
        if (awaitingPermissionResult && !permissionState.canDeliver) {
            showPermissionDeniedDialog = true
        }
        awaitingPermissionResult = false
    }

    GameDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (showPermissionDeniedDialog) {
        NotificationPermissionDeniedDialog(onDismiss = { showPermissionDeniedDialog = false })
    }
}

@Composable
internal fun GameDetailContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        GameDetailMainContent(
            uiState = uiState,
            onEvent = onEvent,
            onBackClick = onBackClick
        )

        uiState.wishlistSelectorState?.let { selectorState ->
            ListSelectorSheet(
                gameName = selectorState.gameName,
                list = selectorState.availableLists,
                onDismiss = { onEvent(GameDetailUiEvent.DismissListSelector) },
                onConfirm = { onEvent(GameDetailUiEvent.ConfirmListSelection) },
                onToggleList = { listId ->
                    onEvent(GameDetailUiEvent.ToggleGameInList(listId))
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MaterialTheme.spacing.extraLarge)
        )
    }
}

@QuestLogPreviews
@Composable
fun GameDetailContentSuccessPreview() {
    QuestLogTheme(darkTheme = isSystemInDarkTheme()) {
        GameDetailContent(
            uiState = GameDetailUiState(
                contentState = GameDetailContentState.Success(GameDetailUiModel.getDummy())
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
fun GameDetailContentLoadingPreview() {
    QuestLogTheme(darkTheme = isSystemInDarkTheme()) {
        GameDetailContent(
            uiState = GameDetailUiState(contentState = GameDetailContentState.Loading),
            onEvent = {},
            onBackClick = {}
        )
    }
}
