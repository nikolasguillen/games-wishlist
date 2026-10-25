package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal actual fun renderLocalDate(date: LocalDate, pattern: String): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

internal actual fun renderLocalDate(date: LocalDate, style: DateStyle): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(style.toFormatStyle()).withLocale(Locale.getDefault()))

internal actual fun renderLocalDateTime(dateTime: LocalDateTime, dateStyle: DateStyle, timeStyle: TimeStyle): String =
    java.time.LocalDateTime.of(
        dateTime.year, dateTime.month.number, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second
    ).format(
        DateTimeFormatter.ofLocalizedDateTime(dateStyle.toFormatStyle(), timeStyle.toFormatStyle())
            .withLocale(Locale.getDefault())
    )

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, month.number, day)

private fun DateStyle.toFormatStyle(): FormatStyle = when (this) {
    DateStyle.SHORT -> FormatStyle.SHORT
    DateStyle.MEDIUM -> FormatStyle.MEDIUM
    DateStyle.LONG -> FormatStyle.LONG
    DateStyle.FULL -> FormatStyle.FULL
}

private fun TimeStyle.toFormatStyle(): FormatStyle = when (this) {
    TimeStyle.SHORT -> FormatStyle.SHORT
    TimeStyle.MEDIUM -> FormatStyle.MEDIUM
}
