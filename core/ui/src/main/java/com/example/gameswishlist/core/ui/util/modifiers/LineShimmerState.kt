package com.example.gameswishlist.core.ui.util.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

/**
 * State class to hold the text layout information required for per-line shimmer.
 */
@Stable
class LineShimmerState {
    var textLayoutResult by mutableStateOf<TextLayoutResult?>(null)
        private set

    fun updateLayout(result: TextLayoutResult) {
        textLayoutResult = result
    }
}

/**
 * Creates and remembers a [LineShimmerState].
 */
@Composable
fun rememberLineShimmerState(): LineShimmerState {
    return remember { LineShimmerState() }
}
