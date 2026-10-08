package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A fancy loading animation that draws a moving, elastic segment along the outline of a game controller.
 *
 * @param modifier Modifier for the animation container.
 * @param color Color of the animated stroke.
 * @param strokeWidth Width of the animated stroke.
 * @param animationDuration Duration of one full loop of the animation in milliseconds.
 * @param repeat Whether the segment keeps circling. `false` plays one pass and then settles on the full
 * outline, for places where the controller is an illustration rather than a sign that something is
 * loading.
 */
@Composable
fun ControllerLoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    animationDuration: Int = 2000,
    repeat: Boolean = true
) {
    val backgroundGhostColor = color.copy(alpha = 0.4f)

    // Head of the segment (leading edge) and tail (trailing edge). The tail starts slow and ends fast, so
    // it catches up with the head.
    val headProgress = remember { Animatable(0f) }
    val tailProgress = remember { Animatable(0f) }
    // How much of the full outline is lit once a single pass has finished. Never moves while looping.
    val settledProgress = remember { Animatable(0f) }

    LaunchedEffect(animationDuration, repeat) {
        do {
            headProgress.snapTo(0f)
            tailProgress.snapTo(0f)
            coroutineScope {
                launch { headProgress.animateTo(1f, tween(animationDuration, easing = HeadEasing)) }
                launch { tailProgress.animateTo(1f, tween(animationDuration, easing = TailEasing)) }
            }
        } while (repeat)
        settledProgress.animateTo(1f, tween(SETTLE_DURATION_MILLIS))
    }

    Canvas(modifier = modifier) {
        val margin = 0.05f
        val ew = size.width * (1 - 2 * margin)
        val eh = size.height * (1 - 2 * margin)
        val sx = size.width * margin
        val sy = size.height * margin
        fun x(p: Float) = sx + (p * ew / 100f)
        fun y(p: Float) = sy + (p * eh / 100f)

        val path = createControllerPath(size)
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, forceClosed = true)

        val totalLength = pathMeasure.length

        // Calculate start and end distances on the path
        // Removed minLength to allow the segment to compress completely at start/end
        val startDistance = (tailProgress.value * totalLength)
        val endDistance = (headProgress.value * totalLength)

        // Empty while the tail has not left the start or has caught up with the head. Drawing the
        // wrapped-around segment there instead lit the whole outline for a frame.
        val segmentPath = Path()
        if (startDistance < endDistance) {
            pathMeasure.getSegment(startDistance, endDistance, segmentPath, true)
        }

        // Draw the background "ghost" details
        drawPath(
            path = path,
            color = backgroundGhostColor,
            style = Stroke(
                width = (strokeWidth / 2f).toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Analog Sticks
        val stickRadius = ew * 0.07f
        drawCircle(
            color = backgroundGhostColor,
            radius = stickRadius,
            center = Offset(x(32f), y(60f)),
            style = Stroke(width = (strokeWidth / 2f).toPx())
        )
        drawCircle(
            color = backgroundGhostColor,
            radius = stickRadius,
            center = Offset(x(68f), y(60f)),
            style = Stroke(width = (strokeWidth / 2f).toPx())
        )

        // D-Pad (Left side)
        val dpadSize = ew * 0.05f
        val dpadThickness = ew * 0.02f
        val dpadCenter = Offset(x(25f), y(40f))
        val dpadPath = Path().apply {
            moveTo(dpadCenter.x - dpadThickness, dpadCenter.y - dpadSize)
            lineTo(dpadCenter.x + dpadThickness, dpadCenter.y - dpadSize)
            lineTo(dpadCenter.x + dpadThickness, dpadCenter.y - dpadThickness)
            lineTo(dpadCenter.x + dpadSize, dpadCenter.y - dpadThickness)
            lineTo(dpadCenter.x + dpadSize, dpadCenter.y + dpadThickness)
            lineTo(dpadCenter.x + dpadThickness, dpadCenter.y + dpadThickness)
            lineTo(dpadCenter.x + dpadThickness, dpadCenter.y + dpadSize)
            lineTo(dpadCenter.x - dpadThickness, dpadCenter.y + dpadSize)
            lineTo(dpadCenter.x - dpadThickness, dpadCenter.y + dpadThickness)
            lineTo(dpadCenter.x - dpadSize, dpadCenter.y + dpadThickness)
            lineTo(dpadCenter.x - dpadSize, dpadCenter.y - dpadThickness)
            lineTo(dpadCenter.x - dpadThickness, dpadCenter.y - dpadThickness)
            close()
        }
        drawPath(
            path = dpadPath,
            color = backgroundGhostColor,
            style = Stroke(
                width = (strokeWidth / 3f).toPx(),
                join = StrokeJoin.Round
            )
        )

        // Action Buttons (Right side - Cross layout)
        val btnDist = ew * 0.045f
        val btnRadius = ew * 0.015f
        val rightCenter = Offset(x(75f), y(40f))
        val btnOffsets = listOf(
            Offset(0f, -btnDist), // Top
            Offset(0f, btnDist),  // Bottom
            Offset(-btnDist, 0f), // Left
            Offset(btnDist, 0f)   // Right
        )
        btnOffsets.forEach { offset ->
            drawCircle(
                color = backgroundGhostColor,
                radius = btnRadius,
                center = rightCenter + offset,
                style = Stroke(width = (strokeWidth / 3f).toPx())
            )
        }

        // The animated segment
        drawPath(
            path = segmentPath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Where a single pass comes to rest: the whole outline, faded in once the sweep is over.
        if (settledProgress.value > 0f) {
            drawPath(
                path = path,
                color = color.copy(alpha = settledProgress.value),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

private val HeadEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

// Tail starts slow and ends fast to catch up with the head.
private val TailEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val SETTLE_DURATION_MILLIS = 400

/**
 * Creates a [Path] representing a game controller outline, scaled to the given [size].
 */
private fun createControllerPath(size: Size): Path {
    val path = Path()
    val w = size.width
    val h = size.height

    // Use a small margin to prevent clipping
    val margin = 0.05f
    val ew = w * (1 - 2 * margin)
    val eh = h * (1 - 2 * margin)
    val sx = w * margin
    val sy = h * margin

    fun x(p: Float) = sx + (p * ew / 100f)
    fun y(p: Float) = sy + (p * eh / 100f)

    // A more faithful silhouette inspired by modern gaming controllers (like DualSense/Xbox)
    // 1. Top Edge - Centered
    path.moveTo(x(50f), y(20f))
    path.lineTo(x(65f), y(20f))

    // 2. Right Shoulder / Triggers area
    path.quadraticTo(x(85f), y(20f), x(92f), y(35f))

    // 3. Right Handle - Outer Curve
    path.cubicTo(x(100f), y(50f), x(100f), y(80f), x(85f), y(92f))

    // 4. Right Handle - Bottom Tip
    path.quadraticTo(x(75f), y(96f), x(70f), y(85f))

    // 5. Right Handle - Inner Curve (S-like curve towards center)
    path.cubicTo(x(65f), y(75f), x(60f), y(65f), x(50f), y(65f))

    // 6. Left Handle - Inner Curve (Symmetric)
    path.cubicTo(x(40f), y(65f), x(35f), y(75f), x(30f), y(85f))

    // 7. Left Handle - Bottom Tip
    path.quadraticTo(x(25f), y(96f), x(15f), y(92f))

    // 8. Left Handle - Outer Curve
    path.cubicTo(x(0f), y(80f), x(0f), y(50f), x(8f), y(35f))

    // 9. Left Shoulder / Triggers back to top
    path.quadraticTo(x(15f), y(20f), x(35f), y(20f))
    path.lineTo(x(50f), y(20f))

    path.close()
    return path
}

@QuestLogPreviews
@Composable
private fun ControllerLoadingAnimationSinglePassPreview() {
    QuestLogTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ControllerLoadingAnimation(
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.primary,
                repeat = false
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun ControllerLoadingAnimationPreview() {
    QuestLogTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ControllerLoadingAnimation(
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
