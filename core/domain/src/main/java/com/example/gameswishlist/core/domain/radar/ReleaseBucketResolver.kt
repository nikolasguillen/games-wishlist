package com.example.gameswishlist.core.domain.radar

import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.ReleaseBucket
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Resolves which [ReleaseBucket] a saved game's release date falls into, relative to [now]. Boundaries are
 * calendar-relative (this ISO week, this calendar month, the 3 calendar months after it) rather than
 * rolling windows, since the bucket names read as calendar concepts.
 *
 * Returns `null` when [releaseDateEpochSeconds] falls strictly before [now]'s calendar day: an
 * already-released saved game is dropped from Radar entirely, not shown in a bucket of its own.
 */
fun resolveBucket(
    releaseDateEpochSeconds: Long?,
    precision: DatePrecision,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): ReleaseBucket? {
    if (precision == DatePrecision.TBD || releaseDateEpochSeconds == null) return ReleaseBucket.TBA

    val today = now.toLocalDateTime(timeZone).date
    val releaseDate = Instant.fromEpochSeconds(releaseDateEpochSeconds).toLocalDateTime(timeZone).date

    if (releaseDate < today) return null

    // DayOfWeek.MONDAY has ordinal 0 in both kotlinx-datetime and java.time, so ordinal + 1 is the ISO
    // day number (Monday = 1 .. Sunday = 7) without depending on which DayOfWeek this target resolves to.
    val isoDayNumber = today.dayOfWeek.ordinal + 1
    val endOfThisWeek = today.plus(7 - isoDayNumber, DateTimeUnit.DAY)
    if (releaseDate <= endOfThisWeek) return ReleaseBucket.THIS_WEEK

    val endOfThisMonth = LocalDate(today.year, today.monthNumber, 1)
        .plus(1, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY)
    if (releaseDate <= endOfThisMonth) return ReleaseBucket.THIS_MONTH

    val endOfNext3Months = LocalDate(today.year, today.monthNumber, 1)
        .plus(4, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY)
    if (releaseDate <= endOfNext3Months) return ReleaseBucket.NEXT_3_MONTHS

    return ReleaseBucket.LATER
}
