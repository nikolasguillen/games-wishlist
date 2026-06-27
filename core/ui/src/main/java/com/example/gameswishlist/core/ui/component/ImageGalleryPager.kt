package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.util.fadingEdge
import kotlin.math.abs

/**
 * A reusable image gallery pager with dot indicators.
 *
 * @param images The list of image URLs to display.
 * @param pagerState The state of the pager.
 * @param modifier The modifier to be applied to the pager.
 * @param contentScale How the images should be scaled.
 * @param onImageClick Optional callback when an image is clicked, receiving the index.
 */
@Composable
fun ImageGalleryPager(
    images: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onImageClick: ((Int) -> Unit)? = null
) {
    val shouldShowPageIndicator = images.size > 1
    Box(modifier = modifier) {
        if (images.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (shouldShowPageIndicator) {
                                Modifier.fadingEdge(bottomAlpha = 1f, fadeSize = 80.dp)
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (onImageClick != null) {
                                Modifier.clickable { onImageClick(page) }
                            } else Modifier
                        )
                )
            }

            if (shouldShowPageIndicator) {
                CustomPagerIndicator(
                    pagerState,
                    currentIndicatorColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

    }
}

@Composable
fun CustomPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    currentIndicatorColor: Color,
    indicatorColor: Color
) {
    val spacing = MaterialTheme.spacing
    val baseWidth = spacing.medium
    val activeWidth = spacing.extraLarge
    val gap = spacing.small
    val height = spacing.medium

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scrollPosition by remember {
            derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
        }

        repeat(pagerState.pageCount) { index ->
            val distance = abs(index - scrollPosition)
            val width = lerp(
                start = baseWidth,
                stop = activeWidth,
                fraction = (1f - distance.coerceIn(0f, 1f))
            )

            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(CircleShape)
                    .background(if (distance < 0.5f) currentIndicatorColor else indicatorColor)
            )
        }
    }
}
