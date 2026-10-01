package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme

private val DEFAULT_ICON_SIZE = 24.dp
private val DEFAULT_OUTLINE_WIDTH = 2.dp

/**
 * An icon with an optional border traced from its own path data, so it hugs the icon's exact
 * silhouette (e.g. a heart's notch and point) rather than a bounding circle or box.
 *
 * The border expands *inward*: a [Stroke] is drawn centered on the path, clipped to the path's
 * own interior via [clipPath], so only its inner half survives. A stroke follows the path's
 * actual contour at a constant perpendicular distance everywhere — unlike scaling the shape
 * toward its center, which distorts anything that isn't roughly circular (a heart's notch and
 * point end up with very different border widths). The icon's outer silhouette is identical
 * whether or not [outlineColor] is set, and never grows.
 */
@Composable
fun CustomOutlinedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    iconSize: Dp = DEFAULT_ICON_SIZE,
    outlineWidth: Dp = DEFAULT_OUTLINE_WIDTH
) {
    val density = LocalDensity.current
    val sizePx = with(density) { iconSize.toPx() }
    val outlineWidthPx = with(density) { outlineWidth.toPx() }
    val path = remember(imageVector, sizePx) { imageVector.toScaledPath(sizePx) }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.size(iconSize).then(semanticsModifier)) {
        drawPath(path, color = tint)
        if (outlineColor != null) {
            // A centered stroke straddles the path edge; clipping to the path's own interior
            // discards the outer half, leaving only an inward-facing band. The width is doubled
            // so the surviving inner half measures outlineWidthPx. Round joins matter here: the
            // default miter join spikes proportionally to 1/sin(angle/2) at a sharp concave vertex
            // (a heart's notch) and can blow past the icon's own bounds, swallowing most of the
            // fill — round joins cap the join at the stroke's own radius instead.
            clipPath(path) {
                drawPath(
                    path,
                    color = outlineColor,
                    style = Stroke(width = outlineWidthPx * 2, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * Flattens every [VectorPath] in this [ImageVector]'s node tree into a single [Path], scaled from
 * the vector's own viewport units up to [targetSizePx] — matching the size Compose's `Icon` would
 * render it at.
 */
private fun ImageVector.toScaledPath(targetSizePx: Float): Path {
    val path = Path()

    fun collect(group: VectorGroup) {
        group.forEach { node ->
            when (node) {
                is VectorPath -> path.addPath(PathParser().addPathNodes(node.pathData).toPath())
                is VectorGroup -> collect(node)
            }
        }
    }
    collect(root)

    val scale = targetSizePx / viewportWidth
    path.transform(Matrix().apply { scale(x = scale, y = scale) })
    return path
}

@Preview(showBackground = true)
@Composable
private fun CustomOutlinedIconPreview() {
    QuestLogTheme {
        CustomOutlinedIcon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            outlineColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomOutlinedIconNoOutlinePreview() {
    QuestLogTheme {
        CustomOutlinedIcon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
