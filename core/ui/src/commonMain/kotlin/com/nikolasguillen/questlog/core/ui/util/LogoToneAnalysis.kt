package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** A pixel needs at least this alpha to count as part of the logo; softer ones are anti-aliasing. */
private const val VISIBLE_FROM_ALPHA = 200

/** A picture with less visible content than this share of its pixels is blank, not light or dark. */
private const val MIN_VISIBLE_SHARE = 0.01f

/** A logo whose visible pixels average above this relative luminance (0 black, 1 white) is a light one. */
private const val LIGHT_ABOVE_LUMINANCE = 0.5f

/**
 * Whether the logo in [pixels] (packed ARGB, row by row) is light or dark, from the mean luminance of the pixels that
 * are drawn. Null when nothing is drawn, so the caller keeps its default.
 */
fun analyseLogoTone(pixels: IntArray): LogoTone? {
    var visible = 0
    var luminanceSum = 0f
    for (pixel in pixels) {
        if (pixel ushr 24 >= VISIBLE_FROM_ALPHA) {
            visible++
            luminanceSum += Color(red = (pixel shr 16) and 0xFF, green = (pixel shr 8) and 0xFF, blue = pixel and 0xFF).luminance()
        }
    }
    if (pixels.isEmpty() || visible.toFloat() / pixels.size < MIN_VISIBLE_SHARE) return null
    return if (luminanceSum / visible > LIGHT_ABOVE_LUMINANCE) LogoTone.LIGHT else LogoTone.DARK
}
