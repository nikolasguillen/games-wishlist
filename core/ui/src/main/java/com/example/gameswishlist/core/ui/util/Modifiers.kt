package com.example.gameswishlist.core.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
