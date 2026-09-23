package com.example.gameswishlist.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Universal date utility for formatting dates across the application.
 * Supports Unix timestamps and ISO-8601 strings, adapting to the device locale.
 */
object DateUtils {

    /**
     * Formats a Unix timestamp (in seconds) to a localized string.
     * @param timestampSeconds The timestamp in seconds.
     * @param style The [FormatStyle] to use (default is MEDIUM).
     * @param locale The [Locale] to use (default is device locale).
     */
    fun formatUnixTimestamp(
        timestampSeconds: Long,
        style: FormatStyle = FormatStyle.MEDIUM,
        locale: Locale = Locale.getDefault()
    ): String {
        val instant = Instant.ofEpochSecond(timestampSeconds)
        val formatter = DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }

    /**
     * Formats a Unix timestamp (in seconds) using a specific pattern.
     */
    fun formatUnixTimestamp(
        timestampSeconds: Long,
        pattern: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val instant = Instant.ofEpochSecond(timestampSeconds)
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }

    /**
     * Formats an ISO-8601 date string (yyyy-MM-dd) to a localized string.
     */
    fun formatIsoDate(
        isoDate: String?,
        style: FormatStyle = FormatStyle.MEDIUM,
        locale: Locale = Locale.getDefault()
    ): String? {
        if (isoDate.isNullOrEmpty()) return null
        return try {
            val date = LocalDate.parse(isoDate)
            val formatter = DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
            date.format(formatter)
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
     * Converts a Unix timestamp to [LocalDate].
     */
    fun timestampToLocalDate(timestampSeconds: Long): LocalDate {
        return Instant.ofEpochSecond(timestampSeconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * Inverse of the `"yyyy-MM-dd"` formatting done by [formatUnixTimestamp]: midnight of that calendar day
     * in the device time zone. The zone has to match the one the string was produced in, otherwise the value
     * round-trips to the previous or next day.
     */
    fun isoDateToEpochSeconds(isoDate: String?): Long? =
        parseIsoDate(isoDate)?.atStartOfDay(ZoneId.systemDefault())?.toEpochSecond()
}
