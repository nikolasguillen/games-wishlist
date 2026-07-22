package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
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
 * @param pagerState The state of the pager.
 * @param onImageClick Optional callback when an image is clicked.
 */
@Composable
internal fun GameDetailHeroHeader(
    images: List<String>,
    scrollOffsetProvider: () -> Int,
    height: Dp,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(pageCount = { images.size }),
    onImageClick: (Int) -> Unit = {}
) {
    ImageGalleryPager(
        images = images,
        pagerState = pagerState,
        onImageClick = onImageClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    )
}
