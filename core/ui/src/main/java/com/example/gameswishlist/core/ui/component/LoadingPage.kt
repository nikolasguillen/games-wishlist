package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A full-screen loading page that displays the [ControllerLoadingAnimation] after a short delay.
 * The delay prevents visual flickering for operations that complete very quickly (e.g. local DB fetch).
 *
 * @param modifier The modifier to be applied to the layout.
 * @param delay The delay duration before the loader appears.
 */
@Composable
fun LoadingPage(
    modifier: Modifier = Modifier,
    delay: Duration = Duration.ZERO
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        visible = true
    }

    if (visible) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ControllerLoadingAnimation(
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
