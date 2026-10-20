package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** A pixel with less alpha than this counts as transparent when judging the edge. */
private const val TRANSPARENT_BELOW_ALPHA = 128

/** A pixel needs at least this alpha to count as artwork; softer ones are anti-aliasing, dimmed by the transparency. */
private const val ARTWORK_FROM_ALPHA = 200

/** At least this share of the edge being transparent makes the logo a transparent one. */
private const val TRANSPARENT_EDGE_SHARE = 0.5f

/** A logo with some transparent edge, but less than [TRANSPARENT_EDGE_SHARE], is a mix no single tile suits. */
private const val MIXED_EDGE_SHARE = 0.1f

/** The most common colour must cover at least this share of the edge to be called its colour. */
private const val DOMINANT_EDGE_SHARE = 0.5f

/** A transparent logo with less visible artwork than this share of its pixels is blank, not light or dark. */
private const val MIN_ARTWORK_SHARE = 0.01f

/** Colours are grouped by their top four bits per channel, so noise and compression do not split one colour. */
private const val CHANNEL_SHIFT = 4
private const val BUCKET_BITS = 12
private const val BUCKET_COUNT = 1 shl BUCKET_BITS

/** Below this contrast ratio an artwork is hard to read on the theme's tile (WCAG's 3:1 is too strict for a logo). */
private const val MIN_ARTWORK_CONTRAST = 2f
private const val LUMINANCE_OFFSET = 0.05f

/**
 * Judges a logo from its [pixels] (packed ARGB, [width] by [height], row by row).
 *
 * - An edge that is mostly transparent gives [LogoBackdrop.Artwork], the mean luminance of what is drawn.
 * - An opaque edge where one colour covers at least half of it gives [LogoBackdrop.Solid] with that colour.
 * - Anything else gives [LogoBackdrop.Undetermined].
 *
 * The edge is the outermost ring of pixels, so the caller shrinks a large image first.
 */
fun analyseLogoPixels(pixels: IntArray, width: Int, height: Int): LogoBackdrop {
    if (width <= 0 || height <= 0 || pixels.size < width * height) return LogoBackdrop.Undetermined

    var edgeSize = 0
    var transparentEdge = 0
    val bucketSize = IntArray(BUCKET_COUNT)
    val redSum = LongArray(BUCKET_COUNT)
    val greenSum = LongArray(BUCKET_COUNT)
    val blueSum = LongArray(BUCKET_COUNT)

    forEachEdgeIndex(width, height) { index ->
        val pixel = pixels[index]
        edgeSize++
        if (alphaOf(pixel) < TRANSPARENT_BELOW_ALPHA) {
            transparentEdge++
        } else {
            val bucket = bucketOf(pixel)
            bucketSize[bucket]++
            redSum[bucket] += redOf(pixel).toLong()
            greenSum[bucket] += greenOf(pixel).toLong()
            blueSum[bucket] += blueOf(pixel).toLong()
        }
    }

    val transparentShare = transparentEdge.toFloat() / edgeSize
    if (transparentShare >= TRANSPARENT_EDGE_SHARE) return artworkOf(pixels)
    if (transparentShare > MIXED_EDGE_SHARE) return LogoBackdrop.Undetermined

    val top = bucketSize.indices.maxBy { bucketSize[it] }
    if (bucketSize[top].toFloat() / edgeSize < DOMINANT_EDGE_SHARE) return LogoBackdrop.Undetermined

    val count = bucketSize[top]
    return LogoBackdrop.Solid(
        Color(
            red = (redSum[top] / count).toInt(),
            green = (greenSum[top] / count).toInt(),
            blue = (blueSum[top] / count).toInt()
        )
    )
}

/**
 * The tile colour for a logo, given the two tiles the theme offers: [themeTile], the one that matches the theme, and
 * [contrastTile], the opposite tone. A logo's own edge colour wins; artwork on a transparent edge keeps [themeTile]
 * unless it is too close to it in tone to be read, and then moves to [contrastTile].
 */
fun LogoBackdrop.resolveLogoTileColor(themeTile: Color, contrastTile: Color): Color = when (this) {
    is LogoBackdrop.Solid -> color
    is LogoBackdrop.Artwork -> if (contrastRatio(luminance, themeTile.luminance()) < MIN_ARTWORK_CONTRAST) {
        contrastTile
    } else {
        themeTile
    }
    LogoBackdrop.Undetermined -> themeTile
}

private fun artworkOf(pixels: IntArray): LogoBackdrop {
    var visible = 0
    var luminanceSum = 0f
    for (pixel in pixels) {
        if (alphaOf(pixel) >= ARTWORK_FROM_ALPHA) {
            visible++
            luminanceSum += Color(redOf(pixel), greenOf(pixel), blueOf(pixel)).luminance()
        }
    }
    if (visible.toFloat() / pixels.size < MIN_ARTWORK_SHARE) return LogoBackdrop.Undetermined
    return LogoBackdrop.Artwork(luminanceSum / visible)
}

/** WCAG's contrast ratio between two relative luminances, always at least 1. */
private fun contrastRatio(first: Float, second: Float): Float {
    val lighter = maxOf(first, second)
    val darker = minOf(first, second)
    return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}

/** Calls [action] with the index of every pixel on the outer ring, each once. */
private inline fun forEachEdgeIndex(width: Int, height: Int, action: (Int) -> Unit) {
    for (y in 0 until height) {
        if (y == 0 || y == height - 1) {
            for (x in 0 until width) action(y * width + x)
        } else {
            action(y * width)
            if (width > 1) action(y * width + width - 1)
        }
    }
}

private fun bucketOf(pixel: Int): Int =
    ((redOf(pixel) shr CHANNEL_SHIFT) shl 8) or ((greenOf(pixel) shr CHANNEL_SHIFT) shl 4) or (blueOf(pixel) shr CHANNEL_SHIFT)

private fun alphaOf(pixel: Int): Int = pixel ushr 24
private fun redOf(pixel: Int): Int = (pixel shr 16) and 0xFF
private fun greenOf(pixel: Int): Int = (pixel shr 8) and 0xFF
private fun blueOf(pixel: Int): Int = pixel and 0xFF
