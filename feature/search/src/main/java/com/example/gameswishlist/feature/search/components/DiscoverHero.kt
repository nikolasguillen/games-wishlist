package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.UiConstants
import com.example.gameswishlist.core.ui.util.fadingEdge
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.core.ui.R as CoreUiR

/**
 * The editorial hero for the top anticipated pick: a full-bleed cover with a kicker badge and a
 * title/meta scrim, per the "Direction A" mock. It is the single highest-value element on the
 * cold-start feed, so it gets its own treatment instead of joining a shelf.
 */
@Composable
internal fun DiscoverHero(
    game: GameItemUiModel,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val heroHeight = 240.dp

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clip(MaterialTheme.shapes.large)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.appColors.cardContainerColor,
                    shape = MaterialTheme.shapes.large
                )
                .clickable { onGameClick(game.id) }
        ) {
            if (game.coverImage != null) {
                SubcomposeAsyncImage(
                    model = game.coverImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(MaterialTheme.spacing.doubleLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = CoreUiR.drawable.placeholder,
                                contentDescription = null,
                                contentScale = ContentScale.Fit
                            )
                        }
                    },
                    error = {
                        HeroImageFallback()
                    }
                )
            } else {
                HeroImageFallback()
            }

            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(percent = 50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(MaterialTheme.spacing.large)
            ) {
                Text(
                    text = stringResource(R.string.discover_most_anticipated).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.spacing.mediumLarge,
                        vertical = MaterialTheme.spacing.small
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .fadingEdge(topAlpha = 1f, fadeSize = 30.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(MaterialTheme.spacing.large)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    game.developer?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = UiConstants.METADATA_SEPARATOR,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = game.releaseDateText.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.discover_release_date_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(
                top = MaterialTheme.spacing.small,
                start = MaterialTheme.spacing.small
            )
        )
    }
}

@Composable
private fun HeroImageFallback() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
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

@Preview(showBackground = true)
@Composable
private fun DiscoverHeroPreview() {
    GamesWishlistTheme {
        DiscoverHero(
            game = GameItemUiModel.getDummy().copy(name = "Hollow Knight: Silksong"),
            onGameClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverHeroNoImagePreview() {
    GamesWishlistTheme {
        DiscoverHero(
            game = GameItemUiModel.getDummy()
                .copy(name = "Hollow Knight: Silksong", coverImage = null),
            onGameClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}
