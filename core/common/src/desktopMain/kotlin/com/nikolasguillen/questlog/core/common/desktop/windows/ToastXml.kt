package com.nikolasguillen.questlog.core.common.desktop.windows

import kotlin.time.Instant

/** Identifiers [WindowsToastNotifications] gives its toasts, the toast XML, and the WinRT time format. */
internal object ToastXml {

    private const val TOAST_ID_PREFIX = "g"

    // 100 ns ticks between 0001-01-01 (the WinRT epoch, in UTC) and 1970-01-01.
    private const val SECONDS_FROM_1601_TO_1970 = 11_644_473_600L
    private const val TICKS_PER_SECOND = 10_000_000L

    /** The toast for [gameId]. A click passes `launch` back to the app. [title] and [body] are escaped. */
    fun build(gameId: Int, title: String, body: String): String =
        """<toast launch="gameId=$gameId" activationType="foreground"><visual><binding template="ToastGeneric">""" +
            "<text>${escape(title)}</text><text>${escape(body)}</text></binding></visual></toast>"

    /** A scheduled toast's id is limited to 16 characters; an IGDB id has at most 10 digits. */
    fun toastId(gameId: Int): String = "$TOAST_ID_PREFIX$gameId"

    /** The game of a toast id built by [toastId], or `null` for one that is not ours. */
    fun gameIdOf(toastId: String): Int? =
        toastId.takeIf { it.startsWith(TOAST_ID_PREFIX) }?.drop(TOAST_ID_PREFIX.length)?.toIntOrNull()

    /** A WinRT `DateTime`: 100 ns ticks since 1601-01-01 UTC. */
    fun toTicks(instant: Instant): Long =
        (instant.epochSeconds + SECONDS_FROM_1601_TO_1970) * TICKS_PER_SECOND + instant.nanosecondsOfSecond / 100

    fun fromTicks(ticks: Long): Instant = Instant.fromEpochSeconds(
        epochSeconds = Math.floorDiv(ticks, TICKS_PER_SECOND) - SECONDS_FROM_1601_TO_1970,
        nanosecondAdjustment = Math.floorMod(ticks, TICKS_PER_SECOND) * 100
    )

    private fun escape(text: String): String = buildString(text.length) {
        for (char in text) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
}
