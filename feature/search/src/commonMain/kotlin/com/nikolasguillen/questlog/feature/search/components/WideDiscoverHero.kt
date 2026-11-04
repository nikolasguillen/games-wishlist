package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.gamecard.GenericGameCardLayout
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.resources.choose_list_content_description
import com.nikolasguillen.questlog.core.ui.resources.placeholder
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdge
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_add_to_wishlist
import com.nikolasguillen.questlog.feature.search.resources.discover_in_wishlist
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_view_details
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * The hero of the wide Discover feed: a full-width banner, clearly taller than the phone hero, with the kicker, the
 * title, the `developer · release date` meta line and two actions of its own ("Add to wishlist" and "View details").
 *
 * The cover is not a click target: the actions are the only way in, so a pointer or a keyboard has an explicit
 * control to reach for. Its height comes from [wideHeroHeight], fed with the window's height and the banner's own
 * width, so a banner in a narrow pane stays proportionate. Below [WIDE_HERO_STACKED_BELOW] the actions move under
 * the title instead of sitting beside it.
 */
@Composable
internal fun WideDiscoverHero(
    game: GameItemUiModel,
    onDetailsClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowHeight = LocalWindowInfo.current.containerDpSize.height

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val heroHeight = wideHeroHeight(windowHeight = windowHeight, availableWidth = maxWidth)
        val isStacked = maxWidth < WIDE_HERO_STACKED_BELOW

        // No onClick: the card only clips and fills; the cover itself does nothing when pressed.
        GenericGameCardLayout(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
        ) {
            Box {
                WideHeroCover(coverImage = game.coverImage)

                WideHeroKicker(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(MaterialTheme.spacing.large)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .fadingEdge(topAlpha = 1f, fadeSize = 30.dp)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(MaterialTheme.spacing.large)
                ) {
                    if (isStacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumLarge)) {
                            WideHeroTitleBlock(game = game)
                            WideHeroActions(
                                isSaved = game.isSaved,
                                onDetailsClick = onDetailsClick,
                                onSaveClick = onSaveClick,
                                onSaveLongClick = onSaveLongClick
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
                        ) {
                            WideHeroTitleBlock(game = game, modifier = Modifier.weight(1f))
                            WideHeroActions(
                                isSaved = game.isSaved,
                                onDetailsClick = onDetailsClick,
                                onSaveClick = onSaveClick,
                                onSaveLongClick = onSaveLongClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WideHeroCover(coverImage: String?) {
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
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(MaterialTheme.spacing.doubleLarge),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = CoreUiRes.drawable.placeholder,
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
}

/** The same pill the compact hero draws, so the two layouts read as one feed. */
@Composable
private fun WideHeroKicker(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, MaterialTheme.appColors.accentOnScrim),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.discover_most_anticipated).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.appColors.accentOnScrim,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.mediumLarge,
                vertical = MaterialTheme.spacing.small
            )
        )
    }
}

/** The title and the `developer · release date` line, built as the compact hero builds them. */
@Composable
private fun WideHeroTitleBlock(game: GameItemUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = game.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
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

@Composable
private fun WideHeroActions(
    isSaved: Boolean,
    onDetailsClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier
    ) {
        WideHeroSaveAction(
            isSaved = isSaved,
            onClick = onSaveClick,
            onLongClick = onSaveLongClick
        )

        // Fixed white: the button sits on the dark scrim in both themes, like every other text on a cover.
        OutlinedButton(
            onClick = onDetailsClick,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text = stringResource(Res.string.discover_view_details),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

/**
 * The wide hero's primary action. A tap toggles the game in the default wishlist; a long press opens the list
 * chooser, announced like the heart on the cards. It is a pill rather than a [androidx.compose.material3.Button]
 * because a Material button has no long press.
 */
@Composable
private fun WideHeroSaveAction(
    isSaved: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chooseListLabel = stringResource(CoreUiRes.string.choose_list_content_description)

    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = chooseListLabel
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
                .padding(horizontal = MaterialTheme.spacing.large)
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Text(
                text = stringResource(
                    if (isSaved) Res.string.discover_in_wishlist else Res.string.discover_add_to_wishlist
                ),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

private val previewGame = GameItemUiModel.getDummy().copy(name = "Hollow Knight: Silksong")

@QuestLogPreviews
@Composable
private fun WideDiscoverHeroPreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame,
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverHeroSavedPreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame.copy(isSaved = true),
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverHeroNoImagePreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame.copy(coverImage = null),
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverHeroNoDeveloperPreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame.copy(developer = null),
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverHeroLongTitlePreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame.copy(
                name = "The Legend of an Extraordinarily Long Game Title: Director's Cut, Complete Edition " +
                    "with Every Expansion and Bonus Chapter"
            ),
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun WideDiscoverHeroDesktopPreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame,
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@Preview(widthDp = 560, heightDp = 800)
@Composable
private fun WideDiscoverHeroNarrowPreview() {
    QuestLogTheme {
        WideDiscoverHero(
            game = previewGame,
            onDetailsClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}
