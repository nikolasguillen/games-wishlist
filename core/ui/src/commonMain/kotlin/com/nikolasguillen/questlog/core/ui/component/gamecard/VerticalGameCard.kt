package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.nikolasguillen.questlog.core.ui.util.ColorUtils
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdge

/**
 * @param onLongClickLabel What a long press does, announced to accessibility services. Required: the card
 * does not know what its caller binds the long press to (Search opens the list chooser, a wishlist removes
 * the game), so every caller says it.
 * @param onSaveClick Taps on the save button. `null` hides the button altogether, for screens where it
 * would be misleading: it reflects membership of the *default* wishlist, not of the list on screen.
 */
@Composable
fun VerticalGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    onLongClickLabel: String,
    modifier: Modifier = Modifier,
    onSaveClick: (() -> Unit)? = null,
    onLongClick: () -> Unit = {}
) {
    val cardHeight = 250.dp
    GenericGameCardLayout(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        modifier = modifier
            .width(180.dp)
            .height(cardHeight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GameCoverHeader(
                coverImage = game.coverImage,
                height = cardHeight
            )

            if (onSaveClick != null) {
                SaveToWishlistButton(
                    isSaved = game.isSaved,
                    onSaveClick = onSaveClick,
                    onLongClick = onLongClick,
                    longClickLabel = onLongClickLabel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.spacing.small)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fadingEdge(topAlpha = 1f, fadeSize = 45.dp)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .padding(bottom = MaterialTheme.spacing.medium)
                    .padding(top = 50.dp)
            ) {
                // Fixed white, not the ambient onSurface: this sits on the permanently-dark scrim above,
                // over an arbitrary cover image, so it must stay legible regardless of app appearance.
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                )

                GameMetadataRow(
                    rating = game.rating,
                    developer = game.developer,
                    // releaseYear is null exactly when releaseDateText resolves to the
                    // unknown-release-date fallback -- see CompactGameCard for the full reasoning.
                    releaseYear = game.releaseYear ?: game.releaseDateText.asString()
                )
            }
        }
    }
}

@Composable
private fun GameMetadataRow(
    rating: Int,
    developer: String?,
    releaseYear: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
    ) {
        if (rating > 0) {
            Icon(
                imageVector = ColorUtils.getScoreIcon(rating),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = ColorUtils.getScoreColor(rating)
            )
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = ColorUtils.getScoreColor(rating)
            )
        }

        // Fixed white-based alphas, not the ambient onSurfaceVariant: this row sits on the same
        // permanently-dark scrim as the title above, over an arbitrary cover image.
        if (rating > 0 && (developer != null || releaseYear != null)) {
            Text(
                text = UiConstants.METADATA_SEPARATOR,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        developer?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        if (developer != null && releaseYear != null) {
            Text(
                text = UiConstants.METADATA_SEPARATOR,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        releaseYear?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun GameCardPreview() {
    QuestLogTheme {
        VerticalGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onLongClickLabel = "Choose list",
            onSaveClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun GameCardSavedPreview() {
    QuestLogTheme {
        VerticalGameCard(
            game = GameItemUiModel.getDummy().copy(isSaved = true),
            onClick = {},
            onLongClickLabel = "Choose list",
            onSaveClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun GameCardWithoutSaveButtonPreview() {
    QuestLogTheme {
        VerticalGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onLongClickLabel = "Remove game"
        )
    }
}
