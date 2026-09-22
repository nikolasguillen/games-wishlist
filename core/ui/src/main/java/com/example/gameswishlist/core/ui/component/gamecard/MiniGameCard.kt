package com.example.gameswishlist.core.ui.component.gamecard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.ui.R as CoreUiR

@Composable
fun MiniGameCard(coverImage: String?, modifier: Modifier = Modifier) {
    OutlinedCard(
        border = BorderStroke(1.dp, MaterialTheme.appColors.cardContainerColor),
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
    ) {
        AsyncImage(
            model = coverImage,
            contentDescription = null,
            error = painterResource(CoreUiR.drawable.placeholder),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview
@Composable
private fun MiniGameCardPreview() {
    GamesWishlistTheme {
        MiniGameCard(coverImage = null)
    }
}