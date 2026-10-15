package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

// The only locale-dependent step of DateUtils. Everything else is plain date arithmetic, so this is the one
// place that needs a platform: month and weekday names come from the device locale's own data.

internal expect fun renderLocalDate(date: LocalDate, pattern: String): String

internal expect fun renderLocalDate(date: LocalDate, style: DateStyle): String

internal expect fun renderLocalDateTime(dateTime: LocalDateTime, dateStyle: DateStyle, timeStyle: TimeStyle): String
