package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/** The longer side of the copy the logo is read from; the edge of a larger picture is averaged down to this. */
private const val SAMPLE_LONG_SIDE = 64

/**
 * Draws this painter into a small bitmap and judges it with [analyseLogoPixels]. It touches no image-loading type, so
 * it works on any painter. A painter without a size, or one that cannot be read, gives
 * [LogoBackdrop.Undetermined]. Not a composable and not main-safe by contract: it is cheap, but call it off the main
 * thread from a list.
 *
 * On Android a hardware bitmap cannot be drawn into this software copy, so the request that produced the painter
 * must have hardware bitmaps turned off.
 */
fun Painter.sampleLogoBackdrop(): LogoBackdrop {
    val source = intrinsicSize
    if (!source.isSpecified || source.width <= 0f || source.height <= 0f) return LogoBackdrop.Undetermined

    val scale = SAMPLE_LONG_SIDE / maxOf(source.width, source.height)
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)

    return try {
        val bitmap = ImageBitmap(width, height)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
            with(this@sampleLogoBackdrop) { draw(size) }
        }
        val pixels = IntArray(width * height)
        bitmap.readPixels(pixels, width = width, height = height)
        analyseLogoPixels(pixels, width, height)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        LogoBackdrop.Undetermined
    }
}
