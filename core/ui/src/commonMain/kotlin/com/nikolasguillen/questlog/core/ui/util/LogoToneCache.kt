package com.nikolasguillen.questlog.core.ui.util

/** The largest number of logos remembered; a screen shows a few dozen, so reaching it means a long session. */
private const val MAX_ENTRIES = 256

/**
 * What [sampleLogoTone] found for each logo URL, kept in memory so a card that scrolls back into view does not read
 * its picture again. A logo with no readable tone is not stored. Read and written from the main thread only. Nothing is
 * persisted: the picture itself comes from the image cache.
 */
object LogoToneCache {
    private val entries = mutableMapOf<String, LogoTone>()

    operator fun get(url: String): LogoTone? = entries[url]

    operator fun set(url: String, tone: LogoTone) {
        if (entries.size >= MAX_ENTRIES) entries.clear()
        entries[url] = tone
    }
}
