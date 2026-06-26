package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.ImmersiveDetailLayout
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel

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
    val headerHeight = 350.dp
    
    Box(modifier = modifier.fillMaxSize()) {
        ImmersiveDetailLayout(
            title = game.name,
            onBackClick = onBackClick,
            headerHeight = headerHeight,
            modifier = Modifier.fillMaxSize(),
            heroContent = { scrollOffsetProvider ->
                GameDetailHeroHeader(
                    imageUrl = game.backgroundImage,
                    scrollOffsetProvider = scrollOffsetProvider,
                    height = headerHeight
                )
            }
        ) { scrollState, innerPadding ->
            GameDetailSheetContent(
                game = game,
                scrollState = scrollState,
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
    }
}

/**
 * The scrollable "Sheet" content of the Game Detail screen.
 */
@Composable
private fun GameDetailSheetContent(
    game: GameDetailUiModel,
    scrollState: ScrollState,
    headerHeight: Dp,
    onEvent: (GameDetailUiEvent) -> Unit,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Initial transparent space to show the hero image
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
                .padding(MaterialTheme.spacing.large)
        ) {
            // Sheet Header with Title, Rating, and Genres
            GameDetailSheetHeader(
                name = game.name,
                ratingText = game.ratingText.asString(),
                genres = game.genres
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            // Personal Progress Section
            GameDetailPersonalCard(
                uiModel = game.personalDetails,
                onStatusChange = { onEvent(GameDetailUiEvent.UpdateStatus(it)) },
                onPriorityChange = { onEvent(GameDetailUiEvent.UpdatePriority(it)) },
                onNotesChange = { onEvent(GameDetailUiEvent.UpdateNotes(it)) }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

            // Game Technical Info Section
            GameDetailInfoSection(
                description = game.description,
                platforms = game.platforms
            )

            // Extra padding at the bottom to prevent content from ending up under the pill
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 150.dp))
        }
    }
}
