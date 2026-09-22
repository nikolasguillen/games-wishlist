package com.example.gameswishlist.core.ui.component.gamecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.core.ui.util.UiConstants
import com.example.gameswishlist.core.ui.util.modifiers.fadingEdge

@Composable
fun VerticalGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val cardHeight = 250.dp
    val chooseListLabel = stringResource(R.string.choose_list_content_description)
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
        modifier = modifier
            .width(180.dp)
            .height(cardHeight)
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = chooseListLabel
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GameCoverHeader(
                coverImage = game.coverImage,
                height = cardHeight
            )

            SaveToWishlistButton(
                isSaved = game.isSaved,
                onSaveClick = onSaveClick,
                onLongClick = onLongClick,
                longClickLabel = chooseListLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaterialTheme.spacing.small)
            )

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
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                )

                GameMetadataRow(
                    rating = game.rating,
                    developer = game.developer,
                    releaseYear = game.releaseYear
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

        if (rating > 0 && (developer != null || releaseYear != null)) {
            Text(
                text = UiConstants.METADATA_SEPARATOR,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        developer?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        if (developer != null && releaseYear != null) {
            Text(
                text = UiConstants.METADATA_SEPARATOR,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        releaseYear?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCardPreview() {
    GamesWishlistTheme {
        VerticalGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCardSavedPreview() {
    GamesWishlistTheme {
        VerticalGameCard(
            game = GameItemUiModel.getDummy().copy(isSaved = true),
            onClick = {}
        )
    }
}
