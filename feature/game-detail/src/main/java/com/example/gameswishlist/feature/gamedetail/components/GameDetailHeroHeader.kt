package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

/**
 * A hero image header that remains fixed or moves with parallax effect behind the content.
 *
 * @param imageUrl The URL of the background image.
 * @param scrollOffsetProvider A provider for the current vertical scroll offset in pixels.
 * @param height The height of the hero header.
 * @param modifier The modifier to be applied to the hero header.
 */
@Composable
fun GameDetailHeroHeader(
    imageUrl: String?,
    scrollOffsetProvider: () -> Int,
    height: Dp,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                // Parallax effect: moves slower than the scroll
                // Accessing scrollOffsetProvider().toFloat() here is safe inside graphicsLayer
                translationY = -scrollOffsetProvider().toFloat() * 0.3f
            }
    )
}
