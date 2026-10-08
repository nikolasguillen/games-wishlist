package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Universal date utility for formatting dates across the application.
 * Supports Unix timestamps and ISO-8601 strings, adapting to the device locale.
 */
object DateUtils {

    /**
     * Formats a Unix timestamp (in seconds) to a localized string in the device locale.
     * @param timestampSeconds The timestamp in seconds.
     * @param style The [DateStyle] to use (default is MEDIUM).
     */
    fun formatUnixTimestamp(
        timestampSeconds: Long,
        style: DateStyle = DateStyle.MEDIUM
    ): String = renderLocalDate(timestampToLocalDate(timestampSeconds), style)

    /**
     * Formats a Unix timestamp (in seconds) using a specific pattern, in the device locale.
     */
    fun formatUnixTimestamp(
        timestampSeconds: Long,
        pattern: String
    ): String = renderLocalDate(timestampToLocalDate(timestampSeconds), pattern)

    /**
     * Formats an ISO-8601 date string (yyyy-MM-dd) to a localized string. Input that is not a date comes back
     * unchanged.
     */
    fun formatIsoDate(
        isoDate: String?,
        style: DateStyle = DateStyle.MEDIUM
    ): String? {
        if (isoDate.isNullOrEmpty()) return null
        return try {
            renderLocalDate(LocalDate.parse(isoDate), style)
        } catch (_: Exception) {
            isoDate
        }
    }

    /**
     * Extracts the year from an ISO-8601 date string.
     */
    fun getYearFromIsoDate(isoDate: String?): String? {
        if (isoDate.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(isoDate).year.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses an ISO-8601 string or returns null if invalid.
     */
    fun parseIsoDate(isoDate: String?): LocalDate? {
        if (isoDate.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(isoDate)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a Unix timestamp to [LocalDate] in the device time zone.
     */
    fun timestampToLocalDate(timestampSeconds: Long): LocalDate {
        return Instant.fromEpochSeconds(timestampSeconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    /**
     * Inverse of the `"yyyy-MM-dd"` formatting done by [formatUnixTimestamp]: midnight of that calendar day
     * in the device time zone. The zone has to match the one the string was produced in, otherwise the value
     * round-trips to the previous or next day.
     */
    fun isoDateToEpochSeconds(isoDate: String?): Long? =
        parseIsoDate(isoDate)?.atStartOfDayIn(TimeZone.currentSystemDefault())?.epochSeconds

    /**
     * IGDB's placeholder for "only the year is known": when a release date has no month or day, IGDB fills
     * `first_release_date` with midnight UTC on 31 December of that year. Landing on that exact day is the
     * only signal available once the value has been flattened to a scalar date with no precision of its
     * own, so a fallback built from it can be labelled "year only" instead of showing a made-up day.
     */
    fun isYearOnlyPlaceholder(isoDate: String?): Boolean {
        val date = parseIsoDate(isoDate) ?: return false
        return date.month == Month.DECEMBER && date.day == 31
    }

    /**
     * Epoch-seconds counterpart of the [isYearOnlyPlaceholder] overload taking an ISO string. The check is
     * made in UTC, the zone IGDB fills the placeholder in: east of Greenwich the device's own calendar day
     * agrees anyway, while west of it the same instant reads as 30 December locally and the placeholder
     * would slip through unrecognised.
     */
    fun isYearOnlyPlaceholder(timestampSeconds: Long?): Boolean {
        if (timestampSeconds == null) return false
        val date = Instant.fromEpochSeconds(timestampSeconds).toLocalDateTime(TimeZone.UTC).date
        return date.month == Month.DECEMBER && date.day == 31
    }
}
