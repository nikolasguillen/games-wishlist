package com.nikolasguillen.questlog.core.ui.util.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
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
 * A modifier that applies a horizontal fading edge effect to a [LazyListState].
 * The fade width continuously scales from 0 to [maxFadeSize] over [rampDistance] as the user scrolls,
 * using an eased multi-stop gradient for a feather-soft, gradual transition.
 *
 * @param state The [LazyListState] used to calculate scroll offsets and remaining distance.
 * @param maxFadeSize The maximum width of the fading edge gradient.
 * @param rampDistance The scroll distance over which the fading edge expands to its full width.
 */
fun Modifier.fadingEdgeHorizontal(
    state: LazyListState,
    maxFadeSize: Dp = 32.dp,
    rampDistance: Dp = 48.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val maxFadePx = maxFadeSize.toPx()
        val rampPx = rampDistance.toPx()
        val width = size.width
        if (width <= 0f) return@drawWithContent

        // 1. Calculate start fade expansion fraction based on scroll offset from the start edge
        val startProgress = if (state.firstVisibleItemIndex > 0) {
            1f
        } else {
            (state.firstVisibleItemScrollOffset.toFloat() / rampPx).coerceIn(0f, 1f)
        }
        val startFadePx = maxFadePx * startProgress

        // 2. Calculate end fade expansion fraction based on remaining scroll distance to the end edge
        val layoutInfo = state.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val endFadePx = if (visibleItems.isEmpty()) {
            0f
        } else {
            val lastItem = visibleItems.last()
            if (lastItem.index < layoutInfo.totalItemsCount - 1) {
                maxFadePx
            } else {
                val itemEnd = lastItem.offset + lastItem.size
                val viewportEnd = layoutInfo.viewportEndOffset
                val remainingScroll = (itemEnd - viewportEnd).toFloat().coerceAtLeast(0f)
                val endProgress = (remainingScroll / rampPx).coerceIn(0f, 1f)
                maxFadePx * endProgress
            }
        }

        if (startFadePx <= 0f && endFadePx <= 0f) return@drawWithContent

        val colorStops = mutableListOf<Pair<Float, Color>>()

        if (startFadePx > 0f) {
            val startStop = (startFadePx / width).coerceAtMost(0.5f)
            // Multi-stop quadratic easing for a feather-soft fade at the start edge
            colorStops.add(0f to Color.Black.copy(alpha = 0f))
            colorStops.add((startStop * 0.25f) to Color.Black.copy(alpha = 0.06f))
            colorStops.add((startStop * 0.50f) to Color.Black.copy(alpha = 0.25f))
            colorStops.add((startStop * 0.75f) to Color.Black.copy(alpha = 0.65f))
            colorStops.add(startStop to Color.Black)
        } else {
            colorStops.add(0f to Color.Black)
        }

        if (endFadePx > 0f) {
            val endStart = ((width - endFadePx) / width).coerceAtLeast(0.5f)
            val endWidth = 1f - endStart
            // Multi-stop quadratic easing for a feather-soft fade at the end edge
            colorStops.add(endStart to Color.Black)
            colorStops.add((endStart + endWidth * 0.25f) to Color.Black.copy(alpha = 0.65f))
            colorStops.add((endStart + endWidth * 0.50f) to Color.Black.copy(alpha = 0.25f))
            colorStops.add((endStart + endWidth * 0.75f) to Color.Black.copy(alpha = 0.06f))
            colorStops.add(1f to Color.Black.copy(alpha = 0f))
        } else {
            colorStops.add(1f to Color.Black)
        }

        drawRect(
            brush = Brush.horizontalGradient(*colorStops.toTypedArray()),
            blendMode = BlendMode.DstIn
        )
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

/**
 * Paints the diagonal surface gradient that sits behind cover art, so a cover that is still loading, has
 * failed or is missing reads as a deliberate tile instead of a flat fill. Apply it before any clip or
 * border you want it to follow.
 */
fun Modifier.coverBackground(): Modifier = composed {
    background(
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceContainerLowest
            )
        )
    )
}
