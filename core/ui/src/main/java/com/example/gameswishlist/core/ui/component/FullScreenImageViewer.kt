package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gameswishlist.core.designsystem.theme.spacing

/**
 * A full-screen image viewer that allows paging through a list of images.
 *
 * @param images The list of image URLs.
 * @param initialPage The index of the image to show first.
 * @param onDismiss Callback to close the viewer.
 * @param onPageChange Callback when the page changes in full screen.
 */
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
    onPageChange: (Int) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { images.size }
        )

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                onPageChange(page)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ImageGalleryPager(
                images = images,
                pagerState = pagerState,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = MaterialTheme.spacing.medium)
                    .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}
