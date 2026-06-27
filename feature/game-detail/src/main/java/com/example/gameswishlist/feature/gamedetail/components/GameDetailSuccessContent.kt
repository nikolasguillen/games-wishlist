package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.FullScreenImageViewer
import com.example.gameswishlist.core.ui.component.ImmersiveDetailLayout
import com.example.gameswishlist.core.ui.component.StatusBarProtection
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import kotlinx.coroutines.launch

/**
 * The full success content of the Game Detail screen, including the immersive layout and floating action pill.
 */
@Composable
internal fun GameDetailSuccessContent(
    game: GameDetailUiModel,
    onBackClick: () -> Unit,
    onEvent: (GameDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerHeight = 450.dp
    var fullScreenImageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val pagerState = rememberPagerState(pageCount = { game.images.size })
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        ImmersiveDetailLayout(
            title = game.name.asString(),
            onBackClick = onBackClick,
            headerHeight = headerHeight,
            modifier = Modifier.fillMaxSize(),
            heroContent = { scrollOffsetProvider ->
                GameDetailHeroHeader(
                    images = game.images,
                    scrollOffsetProvider = scrollOffsetProvider,
                    height = headerHeight,
                    pagerState = pagerState,
                    onImageClick = { fullScreenImageIndex = it }
                )
            }
        ) { innerPadding ->
            GameDetailSheetContent(
                game = game,
                headerHeight = headerHeight,
                onEvent = onEvent,
                innerPadding = innerPadding
            )
        }

        // Floating Action Pill
        GameDetailActionPill(
            isFavorite = game.isWishlisted,
            onFavoriteClick = { onEvent(GameDetailUiEvent.ToggleFavorite) },
            onManageListClick = { onEvent(GameDetailUiEvent.OpenListSelector) },
            onShareClick = { onEvent(GameDetailUiEvent.ShareGame) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(MaterialTheme.spacing.large)
                .navigationBarsPadding()
        )

        StatusBarProtection(color = MaterialTheme.colorScheme.surfaceContainer)
    }

    // Full Screen Viewer
    fullScreenImageIndex?.let { index ->
        FullScreenImageViewer(
            images = game.images,
            initialPage = index,
            onDismiss = { fullScreenImageIndex = null },
            onPageChange = { newIndex ->
                // Update the state so rotation keeps the current page
                fullScreenImageIndex = newIndex
                // Sync back to the main pager
                scope.launch {
                    pagerState.scrollToPage(newIndex)
                }
            }
        )
    }
}

/**
 * The scrollable "Sheet" content of the Game Detail screen.
 */
@Composable
private fun GameDetailSheetContent(
    game: GameDetailUiModel,
    headerHeight: Dp,
    onEvent: (GameDetailUiEvent) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height

    Column(modifier = modifier.fillMaxSize()) {
        // Initial transparent space to show the hero image.
        // This Spacer doesn't consume touches, so they pass through to the hero background.
        Spacer(modifier = Modifier.height(headerHeight - MaterialTheme.spacing.extraLarge))

        // "Sheet" with the actual content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(
                        topStart = MaterialTheme.spacing.extraLarge,
                        topEnd = MaterialTheme.spacing.extraLarge
                    )
                )
                .padding(top = MaterialTheme.spacing.large)
        ) {
            val horizontalPadding = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            // Sheet Header with Title, Rating, and Genres
            GameDetailSheetHeader(
                name = game.name,
                gameType = game.gameType,
                ratingText = game.ratingText,
                genres = game.genres,
                modifier = horizontalPadding
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // Personal Progress Section
            GameDetailPersonalCard(
                uiModel = game.personalDetails,
                onStatusChange = { onEvent(GameDetailUiEvent.UpdateStatus(it)) },
                onPriorityChange = { onEvent(GameDetailUiEvent.UpdatePriority(it)) },
                onNotesChange = { onEvent(GameDetailUiEvent.UpdateNotes(it)) },
                modifier = horizontalPadding
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // Game Technical Info Section
            GameDetailInfoSection(
                description = game.description,
                platforms = game.platforms,
                developers = game.developers,
                publishers = game.publishers,
                engines = game.engines,
                releaseDates = game.releaseDates,
                modifier = horizontalPadding
            )

            if (game.relatedGames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
                RelatedGamesSection(
                    relatedGames = game.relatedGames,
                    onGameClick = { onEvent(GameDetailUiEvent.NavigateToGame(it)) }
                )
            }

            // Extra padding at the bottom to prevent content from ending up under the pill
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 120.dp))
        }
    }
}
