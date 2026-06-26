package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel

/**
 * The main scrollable content of the Game Detail screen, including the "Sheet" that slides over the hero image.
 *
 * @param game The UI model of the game details.
 * @param scrollState The scroll state to be synchronized with other components (like parallax header).
 * @param headerHeight The height of the hero header to calculate the initial transparent space.
 * @param onEvent Callback for UI events.
 * @param innerPadding Padding values from the Scaffold.
 * @param modifier The modifier to be applied to the main content.
 */
@Composable
fun GameDetailMainContent(
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

            // Extra padding at the bottom to prevent content from ending up under the nav bar
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 100.dp))
        }
    }
}
