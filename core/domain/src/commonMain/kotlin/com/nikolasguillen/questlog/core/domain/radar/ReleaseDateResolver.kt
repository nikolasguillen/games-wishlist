package com.nikolasguillen.questlog.core.domain.radar

import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.ReleaseDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The local hour a release-day reminder fires at — see research.md §2 for why a fixed morning hour. */
private const val NOTIFICATION_HOUR = 9

/**
 * Per the multi-platform date resolution decision: one release date per platform the user owns that the
 * game has a date for (earliest region date within each owned platform); if there's no selection or no
 * match, falls back to a single entry for the earliest date across all the game's platforms. An empty list
 * means the game has no release date data at all.
 *
 * Shared by [GetRadarTimelineUseCase] and the release-notification use cases so the timeline and a
 * reminder can never resolve a game to two different dates.
 */
fun Game.resolveReleaseDates(ownedPlatformIds: Set<Int>): List<ReleaseDate> {
    val ownedDates = releaseDates.filter { it.platformId in ownedPlatformIds }
    if (ownedDates.isNotEmpty()) {
        return ownedDates
            .groupBy { it.platformId }
            .mapNotNull { (_, datesForPlatform) -> datesForPlatform.minByOrNull { it.date ?: Long.MAX_VALUE } }
    }
    val fallback = releaseDates.minByOrNull { it.date ?: Long.MAX_VALUE } ?: return emptyList()
    return listOf(fallback)
}

/**
 * The instant a release-day reminder should fire at, given a resolved release date: 09:00 local on the
 * release calendar day, [now] when that instant has already passed but the day is still today, or `null`
 * when the day is in the past or [precision] is coarser than [DatePrecision.EXACT_DATE] — a reminder needs
 * a specific day to fire on, and IGDB only gives one at that precision.
 */
fun resolveNotificationInstant(
    releaseDateEpochSeconds: Long?,
    precision: DatePrecision,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Instant? {
    if (precision != DatePrecision.EXACT_DATE || releaseDateEpochSeconds == null) return null

    val today = now.toLocalDateTime(timeZone).date
    val releaseDay = Instant.fromEpochSeconds(releaseDateEpochSeconds).toLocalDateTime(timeZone).date

    if (releaseDay < today) return null

    val scheduledInstant = LocalTime(NOTIFICATION_HOUR, 0).atDate(releaseDay).toInstant(timeZone)
    return if (releaseDay == today && scheduledInstant < now) now else scheduledInstant
}
