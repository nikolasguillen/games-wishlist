package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.gamecard.GameCoverImage
import com.nikolasguillen.questlog.core.ui.component.gamecard.GenericGameCardLayout
import com.nikolasguillen.questlog.core.ui.component.gamecard.SaveToWishlistButton
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.resources.choose_list_content_description
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdge
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

/**
 * A card of a wide Discover shelf: a portrait cover ([WIDE_SHELF_CARD_ASPECT_RATIO]) that fills the width of its
 * slot, with the save heart at the top end and the title and release year over a dark scrim at the bottom.
 *
 * It shows what `CompactGameCard` shows, in the same way (no developer, no score, no genre): a tap anywhere on the
 * card calls [onClick], and the heart toggles the save on a tap and calls [onSaveLongClick] on a long press. The card
 * has no long press of its own.
 */
@Composable
internal fun WideDiscoverShelfCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chooseListLabel = stringResource(CoreUiRes.string.choose_list_content_description)

    GenericGameCardLayout(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WIDE_SHELF_CARD_ASPECT_RATIO)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The card already clips to its own shape, so the cover needs no rounding of its own.
            GameCoverImage(
                coverImage = game.coverImage,
                shape = RectangleShape,
                modifier = Modifier.fillMaxSize()
            )

            SaveToWishlistButton(
                isSaved = game.isSaved,
                onSaveClick = onSaveClick,
                onLongClick = onSaveLongClick,
                longClickLabel = chooseListLabel,
                modifier = Modifier.align(Alignment.TopEnd)
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
                // Fixed white: this sits on the permanently dark scrim over an arbitrary cover, so it must stay
                // legible whatever the app appearance.
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // releaseYear is null exactly when releaseDateText is the unknown-release-date fallback, so an
                // undated anticipated game still shows something, as on the compact card.
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
}

@QuestLogPreviews
@Composable
private fun WideDiscoverShelfCardPreview() {
    QuestLogTheme {
        WideDiscoverShelfCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverShelfCardSavedPreview() {
    QuestLogTheme {
        WideDiscoverShelfCard(
            game = GameItemUiModel.getDummy().copy(isSaved = true),
            onClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverShelfCardLongTitlePreview() {
    QuestLogTheme {
        WideDiscoverShelfCard(
            game = GameItemUiModel.getDummy()
                .copy(name = "An Extraordinarily Long Game Title That Cannot Possibly Fit"),
            onClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverShelfCardNoImagePreview() {
    QuestLogTheme {
        WideDiscoverShelfCard(
            game = GameItemUiModel.getDummy().copy(coverImage = null),
            onClick = {},
            onSaveClick = {},
            onSaveLongClick = {},
            modifier = Modifier
                .width(180.dp)
                .padding(MaterialTheme.spacing.large)
        )
    }
}
