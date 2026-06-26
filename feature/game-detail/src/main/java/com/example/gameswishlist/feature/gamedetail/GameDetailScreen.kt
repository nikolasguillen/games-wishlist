package com.example.gameswishlist.feature.gamedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.feature.gamedetail.components.GameDetailHeader
import com.example.gameswishlist.feature.gamedetail.components.GameDetailInfoSection
import com.example.gameswishlist.feature.gamedetail.components.GameDetailPersonalCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.contentState is GameDetailContentState.Success) {
                        Text(text = uiState.contentState.game.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_content_description)
                        )
                    }
                },
                actions = {
                    if (uiState.contentState is GameDetailContentState.Success) {
                        IconButton(onClick = { onEvent(GameDetailUiEvent.OpenListSelector) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = stringResource(R.string.add_to_list_content_description)
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
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
                    val game = content.game
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        GameDetailHeader(
                            name = game.name,
                            backgroundImageUrl = game.backgroundImage
                        )

                        Column(modifier = Modifier.padding(16.dp)) {
                            GameDetailPersonalCard(
                                uiModel = game.personalDetails,
                                onStatusChange = { onEvent(GameDetailUiEvent.UpdateStatus(it)) },
                                onPriorityChange = { onEvent(GameDetailUiEvent.UpdatePriority(it)) },
                                onNotesChange = { onEvent(GameDetailUiEvent.UpdateNotes(it)) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GameDetailInfoSection(
                                rating = game.rating,
                                metacritic = game.metaCritic,
                                description = game.description,
                                platforms = game.platforms,
                                genres = game.genres
                            )
                        }
                    }
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
                        rating = 95.0,
                        metaCritic = 92,
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
