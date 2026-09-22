package com.example.gameswishlist.core.ui.component.gamecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.gameswishlist.core.ui.util.modifiers.fadingEdge

@Composable
fun CompactGameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onSaveClick: (() -> Unit)? = null
) {
    val cardHeight = 150.dp
    val chooseListLabel = stringResource(R.string.choose_list_content_description)
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
        modifier = modifier
            .height(cardHeight)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
            .clickable(onClick = onClick)
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
                    longClickLabel = chooseListLabel,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

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

@Preview(showBackground = true)
@Composable
private fun CompactGameCardPreview() {
    GamesWishlistTheme {
        CompactGameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {},
            onSaveClick = {},
            onLongClick = {}
        )
    }
}
