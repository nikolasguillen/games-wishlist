package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterFullStyle
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSDateFormatterStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneWithName

private const val HALF_DAY_SECONDS = 12 * 60 * 60L

internal actual fun renderLocalDate(date: LocalDate, pattern: String): String =
    formatter().apply { dateFormat = pattern }.stringFromDate(date.toNoonUtc())

internal actual fun renderLocalDate(date: LocalDate, style: DateStyle): String =
    formatter().apply { dateStyle = style.toNativeStyle() }.stringFromDate(date.toNoonUtc())

// The date and time are the device's own wall clock, so the formatter works in the device time zone, unlike the
// calendar-date formatter below.
internal actual fun renderLocalDateTime(dateTime: LocalDateTime, dateStyle: DateStyle, timeStyle: TimeStyle): String =
    NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        this.dateStyle = dateStyle.toNativeStyle()
        this.timeStyle = timeStyle.toNativeStyle()
    }.stringFromDate(
        NSDate.dateWithTimeIntervalSince1970(
            dateTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds.toDouble()
        )
    )

// A calendar date has no time zone. Building it at noon UTC and formatting it in UTC keeps the day the same
// whatever zone the device is in, which is what the Android implementation gets from `java.time.LocalDate`.
private fun formatter(): NSDateFormatter = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
    timeZone = NSTimeZone.timeZoneWithName("UTC")!!
}

private fun LocalDate.toNoonUtc(): NSDate =
    NSDate.dateWithTimeIntervalSince1970((atStartOfDayIn(TimeZone.UTC).epochSeconds + HALF_DAY_SECONDS).toDouble())

private fun DateStyle.toNativeStyle(): NSDateFormatterStyle = when (this) {
    DateStyle.SHORT -> NSDateFormatterShortStyle
    DateStyle.MEDIUM -> NSDateFormatterMediumStyle
    DateStyle.LONG -> NSDateFormatterLongStyle
    DateStyle.FULL -> NSDateFormatterFullStyle
}

private fun TimeStyle.toNativeStyle(): NSDateFormatterStyle = when (this) {
    TimeStyle.SHORT -> NSDateFormatterShortStyle
    TimeStyle.MEDIUM -> NSDateFormatterMediumStyle
}
