package com.example.gameswishlist.core.ui.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Applies a high-quality "brushed metal" effect to a composable.
 * Based on: https://www.sinasamaki.com/brushed-metal-ui-in-jetpack-compose/
 *
 * @param shape The shape of the metallic component.
 * @param baseColor The base metallic color. Defaults to theme's primary color.
 * @param animateOnce If true, the highlight rotation will perform a single sweep on entry.
 */
fun Modifier.brushedMetal(
    shape: Shape,
    baseColor: Color? = null,
    animateOnce: Boolean = false
): Modifier = composed {
    val finalBaseColor = baseColor ?: MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    // 1. Prepare Texture Colors (Concentric Rings)
    val ringColor = lerp(finalBaseColor, Color.Black, 0.4f).copy(alpha = 0.3f)
    val ringColors = remember(finalBaseColor) {
        buildList {
            repeat(40) {
                repeat(Random.nextInt(2, 15)) { add(Color.Transparent) }
                repeat(Random.nextInt(1, 3)) { add(ringColor) }
            }
        }
    }

    // 2. Prepare Lighting Colors (Sweep Highlights)
    val highlightColor = lerp(finalBaseColor, Color.White, 0.6f).copy(alpha = 0.4f)
    val highlightColors = remember(finalBaseColor) {
        buildList {
            val count = 4
            add(highlightColor)
            repeat(count) { index ->
                add(Color.Transparent)
                if (index < count - 1) add(highlightColor)
            }
            add(highlightColor)
        }
    }

    // 3. Animation for rotating highlights
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        startAnimation = true
    }

    val rotation by if (animateOnce) {
        animateFloatAsState(
            targetValue = if (startAnimation) 360f else 0f,
            animationSpec = tween(1500, easing = LinearOutSlowInEasing),
            label = "singleRotation"
        )
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "metalRotation")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
    }

    this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, density)
        val path = Path().apply { addOutline(outline) }

        clipPath(path) {
            // Base Layer
            drawRect(color = finalBaseColor)

            // Texture Layer (Radial Rings)
            drawRect(
                brush = Brush.radialGradient(
                    colors = ringColors,
                    center = center,
                    tileMode = TileMode.Repeated
                ),
                blendMode = BlendMode.Overlay
            )

            // Lighting Layer (Rotating Sweep)
            rotate(degrees = rotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = highlightColors,
                        center = center
                    ),
                    radius = size.minDimension * 2,
                    center = center,
                    blendMode = BlendMode.Screen
                )
            }
        }
    }.border(
        width = 1.dp,
        shape = shape,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.3f)
            )
        )
    )
}

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

/**
 * A modifier that applies an animated primary metallic border to a composable.
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
