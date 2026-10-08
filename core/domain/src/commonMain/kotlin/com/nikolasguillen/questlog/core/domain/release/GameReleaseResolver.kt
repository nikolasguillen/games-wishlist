package com.nikolasguillen.questlog.core.domain.release

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Whether the game is out on at least one platform as of [now]'s calendar day, release day included. A
 * game with no usable date anywhere counts as unreleased.
 */
fun Game.isReleased(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    val deadline = releaseDeadline(timeZone) ?: return false
    return deadline <= now.toLocalDateTime(timeZone).date
}

/**
 * Whether the game came out before [now]'s calendar day. Stricter than [isReleased]: on release day itself
 * this is still `false`, because a release-day reminder has that day left to fire on.
 */
fun Game.isPastReleaseDay(now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
    val deadline = releaseDeadline(timeZone) ?: return false
    return deadline < now.toLocalDateTime(timeZone).date
}

/**
 * Whether [status] can be picked for this game: a status that [GameStatus.requiresRelease] stays locked
 * until [isReleased].
 */
fun Game.canSetStatus(
    status: GameStatus,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Boolean = !status.requiresRelease || isReleased(now, timeZone)

/**
 * The first calendar day by which the game is certain to be out somewhere, or `null` when no date is known.
 *
 * Each platform date counts up to the last day its precision allows: "Q4 2026" is only certainly out on 31
 * December 2026, whatever placeholder day IGDB stored for it. The earliest of those across platforms wins.
 * Per-platform dates come first because they carry a precision; [Game.releaseDate] is the fallback for a
 * game with none usable, and only tells a year-only placeholder apart from an exact day.
 */
private fun Game.releaseDeadline(timeZone: TimeZone): LocalDate? =
    releaseDates.mapNotNull { lastPossibleReleaseDay(it.date, it.precision, timeZone) }.minOrNull()
        ?: gameLevelReleaseDeadline()

private fun lastPossibleReleaseDay(epochSeconds: Long?, precision: DatePrecision, timeZone: TimeZone): LocalDate? {
    if (epochSeconds == null) return null
    val instant = Instant.fromEpochSeconds(epochSeconds)
    // IGDB stores a coarse date's placeholder at midnight UTC, so the period it names is read in UTC: in
    // the device zone, a placeholder on the first day of a period would slip into the previous one west of
    // Greenwich.
    val utcDate = instant.toLocalDateTime(TimeZone.UTC).date
    return when (precision) {
        DatePrecision.EXACT_DATE -> instant.toLocalDateTime(timeZone).date
        DatePrecision.YEAR_MONTH -> lastDayOfMonths(utcDate.year, utcDate.month.number, monthCount = 1)
        DatePrecision.QUARTER -> {
            val firstMonthOfQuarter = (utcDate.month.number - 1) / 3 * 3 + 1
            lastDayOfMonths(utcDate.year, firstMonthOfQuarter, monthCount = 3)
        }
        DatePrecision.YEAR_ONLY -> LocalDate(utcDate.year, 12, 31)
        DatePrecision.TBD -> null
    }
}

private fun lastDayOfMonths(year: Int, firstMonth: Int, monthCount: Int): LocalDate =
    LocalDate(year, firstMonth, 1)
        .plus(monthCount, DateTimeUnit.MONTH)
        .minus(1, DateTimeUnit.DAY)

/** [Game.releaseDate] is already a calendar day in the device zone, so it is not converted again. */
private fun Game.gameLevelReleaseDeadline(): LocalDate? {
    val date = releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    return if (DateUtils.isYearOnlyPlaceholder(releaseDate)) LocalDate(date.year, 12, 31) else date
}
