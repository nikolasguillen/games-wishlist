package com.example.gameswishlist.feature.gamedetail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.components.GameDetailMainContent
import com.example.gameswishlist.feature.gamedetail.components.ListSelectorDialog
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEffect
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel

@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBackClick: () -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEffect) {
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
    Box(modifier = modifier.fillMaxSize()) {
        GameDetailMainContent(
            uiState = uiState,
            onEvent = onEvent,
            onBackClick = onBackClick
        )

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
                        name = UiText.DynamicString("The Witcher 3: Wild Hunt"),
                        description = UiText.DynamicString("A legendary RPG with a rich story and vast open world."),
                        images = emptyList(),
                        gameType = UiText.DynamicString("Main Game"),
                        rating = RatingUiModel(95, UiText.DynamicString("Metascore")),
                        platforms = UiText.DynamicString("PC, PS4, Xbox One, Switch"),
                        releaseDates = emptyList(),
                        genres = listOf("RPG", "Action").map { UiText.DynamicString(it) },
                        companyInfo = UiText.DynamicString("CD Projekt Red, CD Projekt"),
                        engines = UiText.DynamicString("RedEngine"),
                        isWishlisted = false,
                        personalDetails = GameDetailPersonalUiModel(
                            notes = UiText.DynamicString("Geralt's adventures are amazing!"),
                            availableStatuses = GameStatus.entries.mapIndexed { index, status ->
                                status.toUiModel(index == 1)
                            },
                            availablePriorities = Priority.entries.mapIndexed { index, priority ->
                                priority.toUiModel(index == 1)
                            }
                        ),
                        relatedGames = emptyList(),
                        mainReleaseDate = UiText.DynamicString("May 19th, 2015")
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
