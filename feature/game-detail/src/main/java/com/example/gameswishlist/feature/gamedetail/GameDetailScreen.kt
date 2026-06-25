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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.feature.gamedetail.components.GameDetailHeader
import com.example.gameswishlist.feature.gamedetail.components.GameDetailInfoSection
import com.example.gameswishlist.feature.gamedetail.components.GameDetailPersonalCard
import com.example.gameswishlist.feature.gamedetail.components.ListSelectorDialog
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel

@Composable
fun GameDetailScreen(
    gameId: Int,
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableLists by viewModel.availableLists.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    GameDetailContent(
        uiState = uiState,
        availableLists = availableLists,
        onBackClick = onBackClick,
        onStatusChange = viewModel::updateStatus,
        onPriorityChange = viewModel::updatePriority,
        onNotesChange = viewModel::updateNotes,
        onAddToList = viewModel::addGameToList,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailContent(
    uiState: GameDetailUiState,
    availableLists: List<WishlistList>,
    onBackClick: () -> Unit,
    onStatusChange: (GameStatus) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onNotesChange: (String) -> Unit,
    onAddToList: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showListSelector by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState is GameDetailUiState.Success) uiState.game.name 
                               else stringResource(R.string.game_detail_title)
                    )
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
                    if (uiState is GameDetailUiState.Success) {
                        IconButton(onClick = { showListSelector = true }) {
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
            when (uiState) {
                is GameDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GameDetailUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is GameDetailUiState.Success -> {
                    val game = uiState.game
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
                                status = game.status,
                                priority = game.priority,
                                notes = game.notes,
                                onStatusChange = onStatusChange,
                                onPriorityChange = onPriorityChange,
                                onNotesChange = onNotesChange
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

        if (showListSelector) {
            ListSelectorDialog(
                lists = availableLists,
                onDismiss = { showListSelector = false },
                onListSelected = { listId ->
                    onAddToList(listId)
                    showListSelector = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailContentSuccessPreview() {
    MaterialTheme {
        GameDetailContent(
            uiState = GameDetailUiState.Success(
                GameDetailUiModel(
                    id = 1,
                    name = "The Witcher 3: Wild Hunt",
                    description = "A legendary RPG with a rich story and vast open world.",
                    backgroundImage = null,
                    rating = 95.0,
                    metaCritic = 92,
                    platforms = listOf("PC", "PS4", "Xbox One", "Switch"),
                    genres = listOf("RPG", "Action"),
                    status = GameStatus.PLAYING,
                    priority = Priority.HIGH,
                    notes = "Geralt's adventures are amazing!"
                )
            ),
            availableLists = listOf(WishlistList(1, "Backlog")),
            onBackClick = {},
            onStatusChange = {},
            onPriorityChange = {},
            onNotesChange = {},
            onAddToList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameDetailContentLoadingPreview() {
    MaterialTheme {
        GameDetailContent(
            uiState = GameDetailUiState.Loading,
            availableLists = emptyList(),
            onBackClick = {},
            onStatusChange = {},
            onPriorityChange = {},
            onNotesChange = {},
            onAddToList = {}
        )
    }
}
