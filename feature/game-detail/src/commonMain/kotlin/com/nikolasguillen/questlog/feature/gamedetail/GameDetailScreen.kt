package com.nikolasguillen.questlog.feature.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.core.ui.util.rememberNotificationOptInRequest
import com.nikolasguillen.questlog.core.ui.util.rememberTextSharer
import com.nikolasguillen.questlog.feature.gamedetail.components.GameDetailMainContent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailContentState
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEffect
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEvent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiState
import kotlinx.coroutines.launch

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
/**
 * A game's detail.
 *
 * @param onBackClick The back button's action; `null` draws no back button.
 * @param onCloseClick The close (X) button's action, drawn in the back button's place when there is none. For a
 * screen that is not the only thing on the display, so it has no history to go back through, only to be dismissed.
 */
@Suppress("ParamsComparedByRef")
@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: (() -> Unit)?,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCloseClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textSharer = rememberTextSharer()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val requestNotificationPermission = rememberNotificationOptInRequest()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is GameDetailUiEffect.ShareGame -> {
                        textSharer.share(effect.text.resolve())
                    }

                    is GameDetailUiEffect.NavigateToGame -> {
                        onGameClick(effect.id)
                    }

                    GameDetailUiEffect.RequestNotificationPermission -> requestNotificationPermission()

                    is GameDetailUiEffect.ShowSnackbar -> {
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(effect.message.resolve())
                        }
                    }
                }
            }
        }
    }

    GameDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        onCloseClick = onCloseClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
internal fun GameDetailContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onCloseClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        GameDetailMainContent(
            uiState = uiState,
            onEvent = onEvent,
            onBackClick = onBackClick,
            onCloseClick = onCloseClick
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
    QuestLogTheme {
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
    QuestLogTheme {
        GameDetailContent(
            uiState = GameDetailUiState(contentState = GameDetailContentState.Loading),
            onEvent = {},
            onBackClick = {}
        )
    }
}
