package com.example.gameswishlist.feature.wishlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.core.ui.R as CoreUiR

@Composable
fun WishlistGameRow(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.mediumLarge
            )
    ) {
        AsyncImage(
            model = game.coverImage,
            contentDescription = null,
            error = painterResource(CoreUiR.drawable.placeholder),
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
        Text(
            text = game.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (game.rating > 0) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Icon(
                imageVector = ColorUtils.getScoreIcon(game.rating),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ColorUtils.getScoreColor(game.rating)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = game.rating.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ColorUtils.getScoreColor(game.rating)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistGameRowPreview() {
    GamesWishlistTheme {
        Surface {
            WishlistGameRow(
                game = GameItemUiModel.getDummy(),
                onClick = {}
            )
        }
    }
}
