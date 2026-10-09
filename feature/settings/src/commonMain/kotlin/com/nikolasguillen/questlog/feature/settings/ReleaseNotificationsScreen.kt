package com.nikolasguillen.questlog.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.component.GameListRow
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.component.NotificationPermissionDeniedDialog
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.back_content_description
import com.nikolasguillen.questlog.core.ui.util.rememberNotificationPermissionState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationUiModel
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsContentState
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEffect
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiEvent
import com.nikolasguillen.questlog.feature.settings.model.ReleaseNotificationsUiState
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.release_notifications_empty
import com.nikolasguillen.questlog.feature.settings.resources.release_notifications_no_date_yet
import com.nikolasguillen.questlog.feature.settings.resources.release_notifications_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun ReleaseNotificationsScreen(
    viewModel: ReleaseNotificationsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState = rememberNotificationPermissionState()
    // LaunchedEffect(viewModel, lifecycle) below never restarts once launched (viewModel/lifecycle are
    // stable for the screen's whole life), so a plain `permissionState` capture would freeze it at
    // whatever canDeliver/isPermanentlyDenied were on first composition -- e.g. staying "permanently
    // denied" forever even after the user grants the permission from system Settings and comes back.
    // rememberUpdatedState keeps the reference the effect closes over live: reading `.value` inside the
    // coroutine always sees the latest permission state instead of the one captured at launch.
    val latestPermissionState = rememberUpdatedState(permissionState)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    // Set only while an opt-in-triggered request is in flight, so the dialog reacts to that specific
    // request's outcome instead of nagging on every screen visit while notifications happen to be off.
    var awaitingPermissionResult by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    ReleaseNotificationsUiEffect.RequestNotificationPermission -> {
                        if (!latestPermissionState.value.canDeliver) {
                            awaitingPermissionResult = true
                            latestPermissionState.value.request()
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

    ReleaseNotificationsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        modifier = modifier
    )

    if (showPermissionDeniedDialog) {
        NotificationPermissionDeniedDialog(onDismiss = { showPermissionDeniedDialog = false })
    }
}

/**
 * Lists every saved game, each with its own release-reminder [Switch] -- a third opt-in surface
 * alongside Radar and the detail screen. Flipping a row never removes it from this list, so there is
 * nothing to confirm before doing it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReleaseNotificationsContent(
    state: ReleaseNotificationsUiState,
    onEvent: (ReleaseNotificationsUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.release_notifications_title),
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
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (val contentState = state.contentState) {
            ReleaseNotificationsContentState.Loading -> LoadingPage(modifier = contentModifier)

            ReleaseNotificationsContentState.Empty -> EmptyPage(
                message = stringResource(Res.string.release_notifications_empty),
                icon = Icons.Filled.NotificationsNone,
                modifier = contentModifier
            )

            is ReleaseNotificationsContentState.Success -> LazyColumn(modifier = contentModifier.fillMaxWidth()) {
                items(contentState.games, key = { it.gameId }) { game ->
                    ReleaseNotificationRow(
                        game = game,
                        onSetEnabled = { enabled -> onEvent(ReleaseNotificationsUiEvent.SetEnabled(game.gameId, enabled)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotificationRow(game: ReleaseNotificationUiModel, onSetEnabled: (Boolean) -> Unit) {
    GameListRow(
        coverImage = game.coverImage,
        title = game.title,
        subtitle = game.dateLabel.asString(),
        onClick = {}
    ) {
        Switch(
            checked = game.isEnabled,
            onCheckedChange = onSetEnabled
        )
    }
}

@QuestLogPreviews
@Composable
private fun ReleaseNotificationsContentSuccessPreview() {
    QuestLogTheme {
        ReleaseNotificationsContent(
            state = ReleaseNotificationsUiState(
                contentState = ReleaseNotificationsContentState.Success(
                    listOf(
                        ReleaseNotificationUiModel(
                            gameId = 1,
                            coverImage = null,
                            title = "Hollow Knight: Silksong",
                            dateLabel = UiText.DynamicString("Sep 25, 2026"),
                            isEnabled = true
                        ),
                        ReleaseNotificationUiModel(
                            gameId = 2,
                            coverImage = null,
                            title = "Grand Theft Auto VI",
                            dateLabel = UiText.StringResource(Res.string.release_notifications_no_date_yet),
                            isEnabled = false
                        )
                    )
                )
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun ReleaseNotificationsContentEmptyPreview() {
    QuestLogTheme {
        ReleaseNotificationsContent(
            state = ReleaseNotificationsUiState(contentState = ReleaseNotificationsContentState.Empty),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun ReleaseNotificationsContentLoadingPreview() {
    QuestLogTheme {
        ReleaseNotificationsContent(
            state = ReleaseNotificationsUiState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}
