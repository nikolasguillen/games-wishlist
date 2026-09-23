package com.nikolasguillen.questlog.core.ui.util.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Draws a dashed rounded-rect border around the composable, for affordances like
 * "tap to create" cards where a solid border would look too permanent/filled-in.
 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp
): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx()))
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}
