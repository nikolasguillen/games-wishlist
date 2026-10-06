package com.nikolasguillen.questlog.core.ui.util.modifiers

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
 * Alpha ramp of one horizontal fade edge, from fully transparent at the viewport edge to fully opaque where
 * the fade ends.
 */
private val FadeEdgeStops = arrayOf(
    0f to Color.Transparent,
    0.25f to Color.Black.copy(alpha = 0.06f),
    0.50f to Color.Black.copy(alpha = 0.25f),
    0.75f to Color.Black.copy(alpha = 0.65f),
    1f to Color.Black
)

/**
 * A modifier that fades the content of a horizontal [LazyListState] out at the edges that still have
 * something to scroll to. Each fade grows from 0 to [maxFadeSize] over [rampDistance] of scrolling, so it
 * appears gradually instead of popping in. It follows the layout direction, so the fade of the list's start
 * lands on the right in RTL. Lists with `reverseLayout = true` are not supported.
 *
 * Scroll state is read in the draw phase only: scrolling redraws the layer but never recomposes.
 *
 * @param state The [LazyListState] used to calculate scroll offsets and remaining distance.
 * @param maxFadeSize The maximum width of each fading edge, capped at half of the composable's width.
 * @param rampDistance The scroll distance over which a fading edge expands to its full width.
 */
fun Modifier.fadingEdgeHorizontal(
    state: LazyListState,
    maxFadeSize: Dp = 32.dp,
    rampDistance: Dp = 48.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val maxFadePx = maxFadeSize.toPx().coerceAtMost(size.width / 2f)
        if (maxFadePx <= 0f) return@drawWithContent
        val rampPx = rampDistance.toPx().coerceAtLeast(1f)

        val leadingFadePx = maxFadePx * state.leadingFadeFraction(rampPx)
        val trailingFadePx = maxFadePx * state.trailingFadeFraction(rampPx)
        val isRtl = layoutDirection == LayoutDirection.Rtl

        drawFadeEdge(fadePx = if (isRtl) trailingFadePx else leadingFadePx, atRight = false)
        drawFadeEdge(fadePx = if (isRtl) leadingFadePx else trailingFadePx, atRight = true)
    }

/** How far the list has been scrolled away from its first item, from 0 (at rest) to 1 (past [rampPx]). */
private fun LazyListState.leadingFadeFraction(rampPx: Float): Float =
    if (firstVisibleItemIndex > 0) 1f
    else (firstVisibleItemScrollOffset / rampPx).coerceIn(0f, 1f)

/** How much content is still hidden past the last item's end, from 0 (at the end) to 1 (over [rampPx]). */
private fun LazyListState.trailingFadeFraction(rampPx: Float): Float {
    val info = layoutInfo
    val lastItem = info.visibleItemsInfo.lastOrNull() ?: return 0f
    if (lastItem.index < info.totalItemsCount - 1) return 1f
    val remaining = (lastItem.offset + lastItem.size - info.viewportEndOffset).coerceAtLeast(0)
    return (remaining / rampPx).coerceIn(0f, 1f)
}

/**
 * Erases the content under a [fadePx]-wide strip on one side of the layer. `DstIn` leaves everything outside
 * the drawn rect untouched, so only the strip itself needs painting.
 */
private fun DrawScope.drawFadeEdge(fadePx: Float, atRight: Boolean) {
    if (fadePx <= 0f) return
    val edgeX = if (atRight) size.width else 0f
    val innerX = if (atRight) size.width - fadePx else fadePx
    drawRect(
        brush = Brush.horizontalGradient(*FadeEdgeStops, startX = edgeX, endX = innerX),
        topLeft = Offset(minOf(edgeX, innerX), 0f),
        size = Size(fadePx, size.height),
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
 * The diagonal surface gradient that sits behind cover art, so a cover that is still loading, has failed or
 * is missing reads as a deliberate tile instead of a flat fill. Paint it with `Modifier.background(brush)`
 * before any clip or border you want it to follow.
 *
 * A brush rather than a modifier because the colors come from [MaterialTheme], which a modifier can only read
 * through `Modifier.composed`.
 */
@Composable
fun rememberCoverBrush(): Brush {
    val start = MaterialTheme.colorScheme.surfaceVariant
    val end = MaterialTheme.colorScheme.surfaceContainerLowest
    return remember(start, end) { Brush.linearGradient(listOf(start, end)) }
}
