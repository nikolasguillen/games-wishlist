package com.nikolasguillen.questlog.core.common

import kotlinx.datetime.LocalDate

// The only locale-dependent step of DateUtils. Everything else is plain date arithmetic, so this is the one
// place that needs a platform: month and weekday names come from the device locale's own data.

internal expect fun renderLocalDate(date: LocalDate, pattern: String): String

internal expect fun renderLocalDate(date: LocalDate, style: DateStyle): String
