package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.gameswishlist.core.ui.component.ImageGalleryPager

/**
 * A hero image gallery header.
 * Parallax and scroll effects are managed by the parent layout.
 *
 * @param images The list of image URLs to display in the gallery.
 * @param scrollOffsetProvider A provider for the current vertical scroll offset in pixels.
 * @param height The height of the hero header.
 * @param modifier The modifier to be applied to the hero header.
 */
@Composable
fun GameDetailHeroHeader(
    images: List<String>,
    scrollOffsetProvider: () -> Int,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    ImageGalleryPager(
        images = images,
        pagerState = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    )
}
