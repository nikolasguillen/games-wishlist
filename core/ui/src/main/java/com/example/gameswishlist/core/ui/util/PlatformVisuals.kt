package com.example.gameswishlist.core.ui.util

import androidx.compose.ui.graphics.Color
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText

/**
 * Resolves a compact display code (max 3 characters) and a distinguishing color for a
 * platform, for use in small icon tiles.
 *
 * Matching is done by IGDB platform ID, since IDs are stable and unambiguous — unlike
 * `abbreviation`, which isn't guaranteed to be 3 characters or shorter (e.g. "Series X|S",
 * "Switch", "Android"), the curated codes below are chosen by hand for readability at tile
 * size. Platforms outside the curated list fall back to a truncated abbreviation/name and a
 * neutral color, so unmapped platforms degrade gracefully instead of breaking.
 */
object PlatformVisuals {

    data class Style(val code: UiText, val color: Color)

    private val DEFAULT_COLOR = Color(0xFF5A5A5A)

    private val PLAYSTATION_BLUE = Color(0xFF2E4EA6)
    private val XBOX_GREEN = Color(0xFF107C10)
    private val NINTENDO_RED = Color(0xFFE60012)

    private val KNOWN_PLATFORMS: Map<Int, Style> = mapOf(
        // PlayStation family
        7 to Style(UiText.StringResource(R.string.platform_code_ps1), PLAYSTATION_BLUE),
        9 to Style(UiText.StringResource(R.string.platform_code_ps3), PLAYSTATION_BLUE),
        48 to Style(UiText.StringResource(R.string.platform_code_ps4), PLAYSTATION_BLUE),
        167 to Style(UiText.StringResource(R.string.platform_code_ps5), PLAYSTATION_BLUE),
        38 to Style(UiText.StringResource(R.string.platform_code_psp), PLAYSTATION_BLUE),
        46 to Style(UiText.StringResource(R.string.platform_code_psv), PLAYSTATION_BLUE),

        // Xbox family
        11 to Style(UiText.StringResource(R.string.platform_code_xbx), XBOX_GREEN),
        12 to Style(UiText.StringResource(R.string.platform_code_x360), XBOX_GREEN),
        49 to Style(UiText.StringResource(R.string.platform_code_xb1), XBOX_GREEN),
        169 to Style(UiText.StringResource(R.string.platform_code_xsx), XBOX_GREEN),

        // Nintendo family
        4 to Style(UiText.StringResource(R.string.platform_code_n64), NINTENDO_RED),
        5 to Style(UiText.StringResource(R.string.platform_code_wii), NINTENDO_RED),
        18 to Style(UiText.StringResource(R.string.platform_code_nes), NINTENDO_RED),
        19 to Style(UiText.StringResource(R.string.platform_code_snes), NINTENDO_RED),
        20 to Style(UiText.StringResource(R.string.platform_code_nds), NINTENDO_RED),
        21 to Style(UiText.StringResource(R.string.platform_code_ngc), NINTENDO_RED),
        37 to Style(UiText.StringResource(R.string.platform_code_3ds), NINTENDO_RED),
        41 to Style(UiText.StringResource(R.string.platform_code_wiiu), NINTENDO_RED),
        130 to Style(UiText.StringResource(R.string.platform_code_switch), NINTENDO_RED),
        508 to Style(UiText.StringResource(R.string.platform_code_switch2), NINTENDO_RED),

        // PC / desktop
        6 to Style(UiText.StringResource(R.string.platform_code_pc), Color(0xFF5E5E5E)),
        14 to Style(UiText.StringResource(R.string.platform_code_mac), Color(0xFF8E8E93)),
        3 to Style(UiText.StringResource(R.string.platform_code_linux), Color(0xFFFCC624)),

        // Mobile
        39 to Style(UiText.StringResource(R.string.platform_code_ios), Color(0xFF555555)),
        34 to Style(UiText.StringResource(R.string.platform_code_android), Color(0xFF3DDC84)),

        // Other
        52 to Style(UiText.StringResource(R.string.platform_code_arcade), Color(0xFFFF9800))
    )

    fun styleFor(platform: Platform): Style {
        return KNOWN_PLATFORMS[platform.id] ?: Style(
            code = UiText.DynamicString((platform.abbreviation ?: platform.name).take(3).uppercase()),
            color = DEFAULT_COLOR
        )
    }
}
