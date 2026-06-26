package com.example.gameswishlist.core.ui.util

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Returns a linear gradient brush that simulates a metallic effect using the theme's primary colors.
 */
@Composable
fun primaryMetallicGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Returns a metallic gradient brush that performs a single "shimmer" animation upon entering the composition.
 *
 * @param durationMillis The duration of the shimmer effect in milliseconds.
 */
@Composable
fun rememberAnimatedMetallicGradient(
    durationMillis: Int = 3000
): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryFixed

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300.milliseconds) // Wait for screen transition to settle
        startAnimation = true
    }

    val progress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis, easing = LinearOutSlowInEasing),
        label = "metallicShimmer"
    )

    return remember(progress, primary, container) {
        Brush.linearGradient(
            colors = listOf(
                primary,
                container,
                primary,
                container,
                primary
            ),
            // We shift the start/end offsets to create the "glitter" movement
            start = Offset(progress * 1000f - 500f, 0f),
            end = Offset(progress * 1000f + 500f, 1000f)
        )
    }
}

/**
 * A modifier that applies an animated primary metallic border to a composable.
 *
 * @param width The width of the border.
 * @param shape The shape of the border.
 */
fun Modifier.animatedMetallicBorder(
    width: Dp = 1.dp,
    shape: Shape
): Modifier = composed {
    this.border(
        width = width,
        brush = rememberAnimatedMetallicGradient(),
        shape = shape
    )
}

/**
 * A modifier that applies a primary metallic border to a composable.
 */
fun Modifier.metallicBorder(
    width: Dp = 1.dp,
    shape: Shape
): Modifier = composed {
    this.border(
        width = width,
        brush = primaryMetallicGradient(),
        shape = shape
    )
}

/**
 * A modifier that applies a primary metallic background to a composable.
 */
fun Modifier.metallicBackground(
    shape: Shape
): Modifier = composed {
    this.background(
        brush = primaryMetallicGradient(),
        shape = shape
    )
}
