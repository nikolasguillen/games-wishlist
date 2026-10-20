package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * What a company logo says about the tile it should sit on, worked out from its pixels by [analyseLogoPixels].
 * It does not depend on the theme, so one result is valid in light and dark; [resolveLogoTileColor] applies it.
 */
@Immutable
sealed interface LogoBackdrop {

    /** An opaque logo whose edge is one colour, so the tile takes that colour and the picture blends into it. */
    data class Solid(val color: Color) : LogoBackdrop

    /**
     * A logo whose edge is transparent, so only the artwork can be judged.
     *
     * @property luminance Mean relative luminance, 0 (black) to 1 (white), of the artwork's visible pixels.
     */
    data class Artwork(val luminance: Float) : LogoBackdrop

    /** An edge of mixed colours (a photo, a gradient), or nothing readable: the theme's tile is the safe choice. */
    data object Undetermined : LogoBackdrop
}
