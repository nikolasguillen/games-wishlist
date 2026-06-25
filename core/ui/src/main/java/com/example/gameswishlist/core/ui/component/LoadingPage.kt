package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A full-screen loading page that displays the [ControllerLoadingAnimation].
 */
@Composable
fun LoadingPage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ControllerLoadingAnimation(
            modifier = Modifier.size(120.dp)
        )
    }
}
