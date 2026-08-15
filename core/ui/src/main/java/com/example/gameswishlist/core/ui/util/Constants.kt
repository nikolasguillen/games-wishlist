package com.example.gameswishlist.core.ui.util

object UiConstants {
    const val MAX_PLATFORM_NAME_LENGTH = 12

    /**
     * Separates two metadata values on one line, as in "CD Projekt Red • 2015". It is punctuation rather
     * than text — nothing to translate — so it stays here instead of in `strings.xml`.
     */
    const val METADATA_SEPARATOR = "•"

    // IGDB Platform Categories
    const val PLATFORM_CATEGORY_CONSOLE = 1
    const val PLATFORM_CATEGORY_OPERATING_SYSTEM = 4

    // IGDB Platform Families
    const val PLATFORM_FAMILY_PLAYSTATION = 1
    const val PLATFORM_FAMILY_XBOX = 2
    const val PLATFORM_FAMILY_SEGA = 3
    const val PLATFORM_FAMILY_LINUX = 4
    const val PLATFORM_FAMILY_NINTENDO = 5

    // Sorting Constants
    const val RECENT_GENERATION_THRESHOLD = 8
}
