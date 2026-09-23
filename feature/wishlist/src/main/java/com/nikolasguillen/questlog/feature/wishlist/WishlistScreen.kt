package com.nikolasguillen.questlog.feature.wishlist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.ui.component.EmptyPage
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.components.DeleteWishlistDialog
import com.nikolasguillen.questlog.feature.wishlist.components.RemoveGameDialog
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistGamesList
import com.nikolasguillen.questlog.feature.wishlist.components.WishlistTopBar
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistContentState
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    WishlistUiEffect.NavigateBack -> onBackClick()
                    is WishlistUiEffect.ShowSnackbar ->
                        snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }

    WishlistContent(
        state = state,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WishlistContent(
    state: WishlistUiState,
    onEvent: (WishlistUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var revealedGameId by remember { mutableStateOf<Int?>(null) }
    var gamePendingRemoval by remember { mutableStateOf<GameItemUiModel?>(null) }

    Scaffold(
        topBar = {
            WishlistTopBar(
                listName = state.listName.asString(),
                canDeleteList = state.canDeleteList,
                onBackClick = onBackClick,
                onDeleteClick = { showDeleteDialog = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (val content = state.contentState) {
            WishlistContentState.Loading -> LoadingPage(modifier = contentModifier)

            WishlistContentState.Empty -> EmptyPage(
                message = stringResource(CoreUiR.string.empty_list_message),
                icon = Icons.Outlined.Inventory2,
                modifier = contentModifier
            )

            is WishlistContentState.Success -> WishlistGamesList(
                sections = content.sections,
                revealedGameId = revealedGameId,
                onRevealedGameIdChange = { revealedGameId = it },
                onGameClick = onGameClick,
                onGameRemoveClick = { game -> gamePendingRemoval = game },
                modifier = contentModifier
            )
        }

        if (showDeleteDialog) {
            DeleteWishlistDialog(
                listName = state.listName.asString(),
                onConfirm = {
                    showDeleteDialog = false
                    onEvent(WishlistUiEvent.OnWishlistDeleted)
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        gamePendingRemoval?.let { game ->
            RemoveGameDialog(
                gameName = game.name,
                onConfirm = {
                    gamePendingRemoval = null
                    revealedGameId = null
                    onEvent(WishlistUiEvent.OnGameRemoved(game.id))
                },
                onDismiss = {
                    gamePendingRemoval = null
                    revealedGameId = null
                }
            )
        }
    }
}

@Composable
private fun WishlistContentPreview(contentState: WishlistContentState) {
    QuestLogTheme {
        WishlistContent(
            state = WishlistUiState(
                listName = UiText.DynamicString("My Wishlist"),
                canDeleteList = true,
                contentState = contentState
            ),
            onEvent = {},
            onGameClick = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistContentSuccessPreview() {
    WishlistContentPreview(
        WishlistContentState.Success(
            sections = listOf(
                WishlistSectionUiModel(
                    status = GameStatus.PLAYING,
                    label = UiText.DynamicString("PLAYING"),
                    games = listOf(GameItemUiModel.getDummy())
                ),
                WishlistSectionUiModel(
                    status = null,
                    label = UiText.DynamicString("NO STATUS"),
                    games = listOf(
                        GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
                    )
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun WishlistContentEmptyPreview() {
    WishlistContentPreview(WishlistContentState.Empty)
}

@Preview(showBackground = true)
@Composable
private fun WishlistContentLoadingPreview() {
    WishlistContentPreview(WishlistContentState.Loading)
}
