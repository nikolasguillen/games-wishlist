package com.nikolasguillen.questlog.core.ui.util

/** The largest number of logos remembered; a screen shows a few dozen, so reaching it means a long session. */
private const val MAX_ENTRIES = 256

/**
 * What [sampleLogoBackdrop] found for each logo URL, kept in memory so a card that scrolls back into view does not
 * read its picture again. The result does not depend on the theme, so it survives a theme change. Read and written
 * from the main thread only. Nothing is persisted: the picture itself comes from the image cache.
 */
object LogoBackdropCache {
    private val entries = mutableMapOf<String, LogoBackdrop>()

    operator fun get(url: String): LogoBackdrop? = entries[url]

    operator fun set(url: String, backdrop: LogoBackdrop) {
        if (entries.size >= MAX_ENTRIES) entries.clear()
        entries[url] = backdrop
    }
}
