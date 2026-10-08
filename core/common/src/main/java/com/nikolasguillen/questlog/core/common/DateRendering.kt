package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// The only locale-dependent step of DateUtils. Everything else is plain date arithmetic, so this is the one
// place that needs a platform: month and weekday names come from the device locale's own data.

internal fun renderLocalDate(date: LocalDate, pattern: String): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

internal fun renderLocalDate(date: LocalDate, style: DateStyle): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(style.toFormatStyle()).withLocale(Locale.getDefault()))

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, month.number, day)

private fun DateStyle.toFormatStyle(): FormatStyle = when (this) {
    DateStyle.SHORT -> FormatStyle.SHORT
    DateStyle.MEDIUM -> FormatStyle.MEDIUM
    DateStyle.LONG -> FormatStyle.LONG
    DateStyle.FULL -> FormatStyle.FULL
}
