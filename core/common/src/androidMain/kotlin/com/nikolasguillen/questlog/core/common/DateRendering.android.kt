package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal actual fun renderLocalDate(date: LocalDate, pattern: String): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

internal actual fun renderLocalDate(date: LocalDate, style: DateStyle): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(style.toFormatStyle()).withLocale(Locale.getDefault()))

private fun LocalDate.toJavaLocalDate(): java.time.LocalDate = java.time.LocalDate.of(year, month.number, day)

private fun DateStyle.toFormatStyle(): FormatStyle = when (this) {
    DateStyle.SHORT -> FormatStyle.SHORT
    DateStyle.MEDIUM -> FormatStyle.MEDIUM
    DateStyle.LONG -> FormatStyle.LONG
    DateStyle.FULL -> FormatStyle.FULL
}
