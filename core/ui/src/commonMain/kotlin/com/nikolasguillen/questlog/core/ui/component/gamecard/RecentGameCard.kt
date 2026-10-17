package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.remove
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdge
import org.jetbrains.compose.resources.stringResource

/** The size [RecentGameCard] has unless the caller gives it another one. */
object RecentGameCardDefaults {
    val Width = 120.dp
    val Height = 150.dp
}

/**
 * A recently viewed game: its cover under a dark scrim with the name and year, and a button to remove it.
 *
 * The card takes its size from [modifier] when one is given, so a grid can let each cell decide; without one it is
 * [RecentGameCardDefaults] sized, which is what a row wants. The cover is as tall as the card.
 */
@Composable
fun RecentGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No modifier given: the default size. Any modifier given owns the size, so the grid can fill its cell instead.
    val cardModifier = if (modifier === Modifier) {
        Modifier.size(RecentGameCardDefaults.Width, RecentGameCardDefaults.Height)
    } else {
        modifier
    }

    BoxWithConstraints(modifier = cardModifier) {
        val cardHeight = maxHeight
        GenericGameCardLayout(
            shape = MaterialTheme.shapes.small,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                GameCoverHeader(
                    coverImage = game.coverImage,
                    height = cardHeight
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fadingEdge(topAlpha = 1f, fadeSize = 30.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(MaterialTheme.spacing.smallMedium)
                        .padding(top = 30.dp)
                ) {
                    // Fixed white, not the ambient onSurface/onSurfaceVariant: this sits on the
                    // permanently-dark scrim above, over an arbitrary cover image, so it must stay
                    // legible regardless of app appearance.
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // releaseYear is null exactly when releaseDateText resolves to the unknown-release-
                    // date fallback -- see CompactGameCard for the full reasoning.
                    Text(
                        text = game.releaseYear ?: game.releaseDateText.asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Removal button
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .padding(MaterialTheme.spacing.small)
                .clickable(onClick = onRemoveClick)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove),
                tint = Color.White,
                modifier = Modifier
                    .padding(MaterialTheme.spacing.small)
                    .fillMaxSize()
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun RecentGameCardPreview() {
    QuestLogTheme {
        RecentGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onRemoveClick = {}
        )
    }
}
