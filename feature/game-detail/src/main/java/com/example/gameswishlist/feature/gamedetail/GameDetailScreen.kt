package com.example.gameswishlist.feature.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.ImmersiveDetailLayout
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.components.GameDetailHeroHeader
import com.example.gameswishlist.feature.gamedetail.components.GameDetailMainContent
import com.example.gameswishlist.feature.gamedetail.components.ListSelectorDialog
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState

@Composable
fun GameDetailScreen(
    gameId: Int,
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        viewModel.onEvent(GameDetailUiEvent.LoadGame(gameId))
    }

    GameDetailContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
fun GameDetailContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerHeight = 350.dp

    ImmersiveDetailLayout(
        title = if (uiState.contentState is GameDetailContentState.Success) uiState.contentState.game.name else "",
        onBackClick = onBackClick,
        headerHeight = headerHeight,
        modifier = modifier,
        actions = { alpha ->
            if (uiState.contentState is GameDetailContentState.Success) {
                IconButton(
                    onClick = { onEvent(GameDetailUiEvent.OpenListSelector) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = (1f - alpha) * 0.4f
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.add_to_list_content_description),
                        tint = if (alpha > 0.5f) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        },
        heroContent = { scrollOffsetProvider ->
            if (uiState.contentState is GameDetailContentState.Success) {
                GameDetailHeroHeader(
                    imageUrl = uiState.contentState.game.backgroundImage,
                    scrollOffsetProvider = scrollOffsetProvider,
                    height = headerHeight
                )
            }
        }
    ) { scrollState, innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val content = uiState.contentState) {
                is GameDetailContentState.Loading -> {
                    LoadingPage()
                }

                is GameDetailContentState.Error -> {
                    Text(
                        text = content.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is GameDetailContentState.Success -> {
                    GameDetailMainContent(
                        game = content.game,
                        scrollState = scrollState,
                        headerHeight = headerHeight,
                        onEvent = onEvent,
                        innerPadding = innerPadding
                    )
                }
            }
        }

        if (uiState.isListSelectorVisible) {
            ListSelectorDialog(
                lists = uiState.availableLists,
                onDismiss = { onEvent(GameDetailUiEvent.DismissListSelector) },
                onListSelected = { listId ->
                    onEvent(GameDetailUiEvent.AddGameToList(listId))
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailContentSuccessPreview() {
    GamesWishlistTheme {
        GameDetailContent(
            uiState = GameDetailUiState(
                contentState = GameDetailContentState.Success(
                    GameDetailUiModel(
                        id = 1,
                        name = "The Witcher 3: Wild Hunt",
                        description = "A legendary RPG with a rich story and vast open world.",
                        backgroundImage = null,
                        ratingText = UiText.DynamicString("Metacritic: 95"),
                        platforms = listOf("PC", "PS4", "Xbox One", "Switch"),
                        genres = listOf("RPG", "Action"),
                        personalDetails = GameDetailPersonalUiModel(
                            notes = "Geralt's adventures are amazing!",
                            availableStatuses = GameStatus.entries.mapIndexed { index, status ->
                                status.toUiModel(index == 1)
                            },
                            availablePriorities = Priority.entries.mapIndexed { index, priority ->
                                priority.toUiModel(index == 1)
                            }
                        )
                    )
                ),
                availableLists = listOf(WishlistList(1, "Backlog"))
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailContentLoadingPreview() {
    GamesWishlistTheme {
        GameDetailContent(
            uiState = GameDetailUiState(contentState = GameDetailContentState.Loading),
            onEvent = {},
            onBackClick = {}
        )
    }
}
