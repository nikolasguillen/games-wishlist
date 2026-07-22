package com.example.gameswishlist.feature.gamedetail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.components.GameDetailMainContent
import com.example.gameswishlist.feature.gamedetail.components.ListSelectorSheet
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.AvailabilityUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEffect
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState
import com.example.gameswishlist.feature.gamedetail.model.PlatformTileUiModel
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
                        rating = RatingUiModel(
                            score = 95,
                            scoreText = UiText.DynamicString("95"),
                            scoreLabel = UiText.DynamicString("Metascore"),
                            hypes = UiText.DynamicString("120"),
                            hypesLabel = UiText.StringResource(R.string.hypes_title),
                            ratingCount = UiText.DynamicString("450"),
                            ratingCountLabel = UiText.StringResource(R.string.rating_count_title)
                        ),
                        availability = AvailabilityUiModel(
                            mainDate = UiText.DynamicString("May 19, 2015"),
                            platforms = listOf(
                                PlatformTileUiModel(id = 1, code = UiText.DynamicString("PC"), color = Color(0xFF5E5E5E)),
                                PlatformTileUiModel(id = 2, code = UiText.DynamicString("PS4"), color = Color(0xFF2E4EA6)),
                                PlatformTileUiModel(id = 3, code = UiText.DynamicString("XB1"), color = Color(0xFF107C10)),
                                PlatformTileUiModel(id = 4, code = UiText.DynamicString("SWI"), color = Color(0xFFE60012))
                            ),
                            detailedDates = emptyList(),
                            isExpandable = false
                        ),
                        genres = listOf("RPG", "Action").map { UiText.DynamicString(it) },
                        companyInfo = UiText.DynamicString("CD Projekt Red, CD Projekt"),
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
                        relatedGames = emptyList()
                    )
                )
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
