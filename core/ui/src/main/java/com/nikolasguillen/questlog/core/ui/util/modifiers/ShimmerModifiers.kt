package com.nikolasguillen.questlog.core.ui.util.modifiers

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Shared constants for shimmer effects to ensure visual consistency across the app.
 */
internal object ShimmerDefaults {
    val Colors = listOf(
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.1f),
        Color.LightGray.copy(alpha = 0.2f),
    )

    val AnimationSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 1300, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
}

/**
 * A modifier that applies a shimmer effect to a composable.
 * Typically used for skeleton loading states.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnim by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = ShimmerDefaults.AnimationSpec,
        label = "shimmerTranslation"
    )

    this.then(
        Modifier
            .onGloballyPositioned { size = it.size }
            .background(
                brush = Brush.linearGradient(
                    colors = ShimmerDefaults.Colors,
                    start = Offset(translateAnim, translateAnim),
                    end = Offset(translateAnim + size.width.toFloat(), translateAnim + size.height.toFloat())
                )
            )
    )
}

/**
 * A modifier that applies a shimmer effect line-by-line to a Text composable,
 * reusing the same visual style as [shimmerEffect].
 *
 * @param state The [LineShimmerState] which must be updated via Text's onTextLayout.
 * @param visible Whether the shimmer is visible. If false, the original content is drawn.
 * @param cornerRadius The corner radius for each line's shimmer block.
 */
fun Modifier.lineShimmer(
    state: LineShimmerState,
    visible: Boolean = true,
    cornerRadius: Dp = 4.dp
): Modifier = composed {
    if (!visible) return@composed Modifier

    val transition = rememberInfiniteTransition(label = "lineShimmer")

    val translateAnim by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = ShimmerDefaults.AnimationSpec,
        label = "shimmerTranslation"
    )

    this.drawWithContent {
        val layout = state.textLayoutResult

        if (layout != null) {
            val width = size.width
            val xOffset = width * translateAnim

            val brush = Brush.linearGradient(
                colors = ShimmerDefaults.Colors,
                start = Offset(xOffset, xOffset),
                end = Offset(xOffset + width, xOffset + size.height)
            )

            for (i in 0 until layout.lineCount) {
                val lineTop = layout.getLineTop(i)
                val lineBottom = layout.getLineBottom(i)
                val lineLeft = layout.getLineLeft(i)
                val lineRight = layout.getLineRight(i)

                val lineHeight = lineBottom - lineTop
                val lineWidth = lineRight - lineLeft

                if (lineWidth > 0) {
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(lineLeft, lineTop + lineHeight * 0.15f),
                        size = Size(lineWidth, lineHeight * 0.7f),
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                }
            }
        } else {
            drawContent()
        }
    }
}
