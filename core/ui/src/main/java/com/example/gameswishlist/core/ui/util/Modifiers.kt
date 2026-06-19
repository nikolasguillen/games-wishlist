package com.example.gameswishlist.core.ui.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A modifier that applies a fading edge effect to the top and/or bottom of a composable.
 * Useful for scrollable lists to avoid harsh edges.
 *
 * @param topAlpha The alpha of the top fade (0f = no fade, 1f = full fade).
 * @param bottomAlpha The alpha of the bottom fade (0f = no fade, 1f = full fade).
 * @param topSolidHeight The height of the completely transparent area at the top before the fade starts.
 * @param bottomSolidHeight The height of the completely transparent area at the bottom before the fade starts.
 * @param fadeSize The size of the gradient fade.
 */
fun Modifier.fadingEdge(
    topAlpha: Float = 0f,
    bottomAlpha: Float = 0f,
    topSolidHeight: Dp = 0.dp,
    bottomSolidHeight: Dp = 0.dp,
    fadeSize: Dp = 16.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val topSolidPx = topSolidHeight.toPx()
        val bottomSolidPx = bottomSolidHeight.toPx()
        val fadeSizePx = fadeSize.toPx()
        val height = size.height

        if (topAlpha > 0f || bottomAlpha > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 1f - topAlpha),
                    topSolidPx / height to Color.Black.copy(alpha = 1f - topAlpha),
                    (topSolidPx + fadeSizePx) / height to Color.Black,
                    (height - bottomSolidPx - fadeSizePx) / height to Color.Black,
                    (height - bottomSolidPx) / height to Color.Black.copy(alpha = 1f - bottomAlpha),
                    1f to Color.Black.copy(alpha = 1f - bottomAlpha)
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * A modifier that applies a shimmer effect to a composable.
 * Typically used for skeleton loading states.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startInPx = -2 * size.width.toFloat()
    val endInPx = 2 * size.width.toFloat()
    
    val translateAnim by transition.animateFloat(
        initialValue = startInPx,
        targetValue = endInPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    this.then(
        Modifier
            .onGloballyPositioned { size = it.size }
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(translateAnim, translateAnim),
                    end = Offset(translateAnim + size.width.toFloat(), translateAnim + size.height.toFloat())
                )
            )
    )
}
