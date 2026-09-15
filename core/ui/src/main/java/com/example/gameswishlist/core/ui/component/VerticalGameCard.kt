package com.example.gameswishlist.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.core.ui.util.UiConstants
import com.example.gameswishlist.core.ui.util.fadingEdge
import kotlinx.coroutines.launch

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
            .clip(RoundedCornerShape(MaterialTheme.spacing.mediumLarge))
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
                modifier = Modifier.align(Alignment.TopEnd)
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

/**
 * Toggles the game's membership in the default wishlist. A tap toggles it directly; a long press
 * (shared with the card underneath it, so it fires regardless of which of the two catches the gesture)
 * opens the list selector instead.
 */
@Composable
private fun SaveToWishlistButton(
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String,
    modifier: Modifier = Modifier
) {
    // Triggered from the click callback rather than derived from isSaved: keying it to the state would
    // replay the pulse on every card that is already saved the moment the grid first composes, and again
    // whenever a scrolled-away card re-enters composition.
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // The touch target (48dp, accessibility minimum) is kept larger than the visible circle (32dp),
    // the same way Material's own IconButton pads a 24dp icon inside a 48dp target.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(MaterialTheme.spacing.small)
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch {
                        scale.animateTo(1.3f, animationSpec = spring(stiffness = Spring.StiffnessHigh))
                        scale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    onSaveClick()
                },
                // combinedClickable already performs HapticFeedbackType.LongPress on its own, so the
                // long-click branch needs no explicit call.
                onLongClick = onLongClick,
                onLongClickLabel = longClickLabel
            )
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .size(32.dp)
                .scale(scale.value)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isSaved) {
                            R.string.remove_from_wishlist_content_description
                        } else {
                            R.string.add_to_wishlist_content_description
                        }
                    ),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(MaterialTheme.spacing.large)
                )
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
    val cardHeight = 150.dp
    Box(
        modifier = modifier
            .width(120.dp)
            .height(cardHeight)
    ) {
        OutlinedCard(
            border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
                .clickable(onClick = onClick)
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
fun GameCompactCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardHeight = 150.dp
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
        modifier = modifier
            .height(cardHeight)
            .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
            .clickable(onClick = onClick)
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
}

@Composable
private fun GameCoverHeader(
    coverImage: String?,
    height: Dp
) {
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
    ) {
        if (coverImage != null) {
            SubcomposeAsyncImage(
                model = coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.extraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = R.drawable.placeholder,
                            contentDescription = null,
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                error = {
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
            )
        } else {
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

@Preview(showBackground = true)
@Composable
private fun RecentGameCardPreview() {
    GamesWishlistTheme {
        RecentGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onRemoveClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCompactCardPreview() {
    GamesWishlistTheme {
        GameCompactCard(
            game = GameItemUiModel.getDummy(),
            onClick = {}
        )
    }
}