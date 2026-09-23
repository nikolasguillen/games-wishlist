package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 */
@Composable
fun rememberAnimatedMetallicGradient(
    durationMillis: Int = 1000
): Brush {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
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
            start = Offset(progress * 3000f - 1500f, 0f),
            end = Offset(progress * 3000f + 1500f, 1000f)
        )
    }
}

private val RainbowMetallicColors = listOf(
    Color(0xFFFF595E), // Metallic Red
    Color(0xFFFFCA3A), // Metallic Yellow
    Color(0xFF8AC926), // Metallic Green
    Color(0xFF1982C4), // Metallic Blue
    Color(0xFF6A4C93), // Metallic Purple
    Color(0xFFFF595E)  // Metallic Red repeat for continuity
)

/**
 * Returns a linear gradient brush that simulates a rainbow metallic effect with a diagonal orientation.
 */
fun rainbowMetallicGradient(): Brush {
    return Brush.linearGradient(
        colors = RainbowMetallicColors,
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
