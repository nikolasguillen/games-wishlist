package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.fadingEdge
import kotlin.math.roundToInt

@Composable
fun VerticalGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.cardContainerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0f)),
        modifier = modifier
            .clip(RoundedCornerShape(MaterialTheme.spacing.mediumLarge))
            .clickable(onClick = onClick)
            .width(180.dp)
    ) {
        Column {
            GameCoverHeader(
                coverImage = game.coverImage,
                rawRating = game.rawRating,
                height = 200.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                modifier = Modifier
                    .padding(vertical = MaterialTheme.spacing.medium)
                    .padding(horizontal = MaterialTheme.spacing.medium)
                    .fillMaxWidth()
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                )

                GameMetadataRow(developer = game.developer, releaseYear = game.releaseYear)
            }
        }
    }
}

@Composable
fun RecentGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(130.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.cardContainerColor),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
                .clickable(onClick = onClick)
        ) {
            Column {
                GameCoverHeader(
                    coverImage = game.coverImage,
                    rawRating = null,
                    height = 150.dp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .padding(MaterialTheme.spacing.smallMedium)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    game.releaseYear?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
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
                contentDescription = stringResource(R.string.remove),
                tint = Color.White,
                modifier = Modifier
                    .padding(MaterialTheme.spacing.small)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun GameCoverHeader(
    coverImage: String?,
    rawRating: Double?,
    height: Dp
) {
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = MaterialTheme.spacing.mediumLarge,
                    topEnd = MaterialTheme.spacing.mediumLarge
                )
            )
    ) {
        if (coverImage != null) {
            AsyncImage(
                model = coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(bottomAlpha = 1f, fadeSize = 40.dp)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(bottomAlpha = 1f, fadeSize = 20.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        if (rawRating != null && rawRating > 0) {
            RatingBadge(
                rating = rawRating,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(MaterialTheme.spacing.small)
            )
        }
    }
}

@Composable
private fun GameMetadataRow(
    developer: com.example.gameswishlist.core.ui.model.UiText?,
    releaseYear: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
    ) {
        developer?.let {
            Text(
                text = it.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        if (developer != null && releaseYear != null) {
            Text(
                text = "•",
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

@Composable
private fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
        shape = RoundedCornerShape(MaterialTheme.spacing.small),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = when {
                    rating >= 80 -> Color(0xFF4CAF50) // Green
                    rating >= 60 -> Color(0xFFFFC107) // Yellow
                    else -> Color(0xFFF44336) // Red
                }
            )
            Text(
                text = rating.roundToInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
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
