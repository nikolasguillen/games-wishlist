package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.GameItemUiModel

@Composable
fun GameCard(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.spacing.large))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.height(150.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = game.coverImage,
                contentDescription = null,
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = MaterialTheme.spacing.medium,
                            bottomStart = MaterialTheme.spacing.medium
                        )
                    ),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = MaterialTheme.spacing.medium)
                    .padding(end = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = game.releaseDateText.asString(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    text = game.rating.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                game.developer?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                game.platforms?.let {
                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameCardPreview() {
    GamesWishlistTheme {
        GameCard(
            game = GameItemUiModel.getDummy(),
            onClick = {}
        )
    }
}
