package com.nikolasguillen.questlog.feature.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.ReleaseBucket
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.component.MainScreenHeader
import com.nikolasguillen.questlog.core.ui.component.NotificationPermissionDeniedDialog
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.util.rememberNotificationPermissionState
import com.nikolasguillen.questlog.feature.radar.components.RadarEmptyState
import com.nikolasguillen.questlog.feature.radar.components.RadarGameRow
import com.nikolasguillen.questlog.feature.radar.components.RadarSectionHeader
import com.nikolasguillen.questlog.feature.radar.model.RadarContentState
import com.nikolasguillen.questlog.feature.radar.model.RadarEntryUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarSectionUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEffect
import com.nikolasguillen.questlog.feature.radar.model.RadarUiEvent
import com.nikolasguillen.questlog.feature.radar.model.RadarUiState
import kotlinx.coroutines.launch

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    // Set only while an opt-in-triggered request is in flight, so the dialog reacts to that specific
    // request's outcome instead of nagging on every screen visit while notifications happen to be off.
    var awaitingPermissionResult by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    RadarUiEffect.RequestNotificationPermission -> {
                        if (!latestPermissionState.value.canDeliver) {
                            awaitingPermissionResult = true
                            latestPermissionState.value.request()
                        }
                    }

                    is RadarUiEffect.ShowSnackbar -> {
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

    RadarContent(
        state = state,
        onGameClick = onGameClick,
        onProfileClick = onProfileClick,
        onToggleNotification = { gameId ->
            viewModel.onEvent(RadarUiEvent.ToggleReleaseNotification(gameId))
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )

    if (showPermissionDeniedDialog) {
        NotificationPermissionDeniedDialog(onDismiss = { showPermissionDeniedDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RadarContent(
    state: RadarUiState,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onToggleNotification: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            MainScreenHeader(
                title = stringResource(R.string.radar_title),
                onProfileClick = onProfileClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        when (val content = state.contentState) {
            RadarContentState.Loading -> LoadingPage(modifier = contentModifier)

            RadarContentState.Empty -> RadarEmptyState(modifier = contentModifier)

            is RadarContentState.Success -> LazyColumn(modifier = contentModifier) {
                content.sections.forEach { section ->
                    stickyHeader(key = "header_${section.bucket}", contentType = "header") {
                        RadarSectionHeader(
                            label = section.label.asString(),
                            bucket = section.bucket,
                            modifier = Modifier
                                .padding(horizontal = MaterialTheme.spacing.large)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.background
                                )
                        )
                    }
                    items(
                        items = section.entries,
                        key = { "${section.bucket}_${it.id}_${it.platform.id}" },
                        contentType = { "game" }
                    ) { entry ->
                        RadarGameRow(
                            entry = entry,
                            onClick = { onGameClick(entry.id) },
                            onToggleNotification = { onToggleNotification(entry.id) },
                            showNotificationToggle = section.bucket != ReleaseBucket.RECENTLY_RELEASED
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarContentPreview(contentState: RadarContentState) {
    QuestLogTheme {
        RadarContent(
            state = RadarUiState(contentState = contentState),
            onGameClick = {},
            onProfileClick = {},
            onToggleNotification = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@QuestLogPreviews
@Composable
private fun RadarContentSuccessPreview() {
    RadarContentPreview(
        RadarContentState.Success(
            sections = listOf(
                RadarSectionUiModel(
                    bucket = ReleaseBucket.THIS_WEEK,
                    label = UiText.DynamicString("This week"),
                    // Same game, two owned platforms: one row per platform, same as GetRadarTimelineUseCase emits.
                    entries = listOf(
                        RadarEntryUiModel.getDummy(),
                        RadarEntryUiModel.getDummy().copy(
                            platform = RadarEntryUiModel.getDummy().platform.copy(
                                id = 6,
                                code = UiText.DynamicString("PC"),
                                color = Color(0xFF5E5E5E)
                            )
                        )
                    )
                ),
                RadarSectionUiModel(
                    bucket = ReleaseBucket.LATER,
                    label = UiText.DynamicString("Later"),
                    entries = listOf(
                        RadarEntryUiModel.getDummy().copy(
                            id = 2,
                            title = "Grand Theft Auto VI",
                            studio = "Rockstar Games",
                            dateLabel = UiText.DynamicString("2027"),
                            dateSubLabel = null,
                            dateStyle = RadarEntryUiModel.DateLabelStyle.PILL_MUTED
                        )
                    )
                )
            )
        )
    )
}

@QuestLogPreviews
@Composable
private fun RadarContentEmptyPreview() {
    RadarContentPreview(RadarContentState.Empty)
}

@QuestLogPreviews
@Composable
private fun RadarContentLoadingPreview() {
    RadarContentPreview(RadarContentState.Loading)
}
