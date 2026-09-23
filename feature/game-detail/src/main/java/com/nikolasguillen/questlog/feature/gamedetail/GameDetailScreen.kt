package com.nikolasguillen.questlog.feature.gamedetail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.feature.gamedetail.components.GameDetailMainContent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailContentState
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEffect
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEvent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiState

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
                }
            }
        }
    }

    GameDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
internal fun GameDetailContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
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
    }
}

@Preview(showBackground = true)
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

@Preview(showBackground = true)
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
