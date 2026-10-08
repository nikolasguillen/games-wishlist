package com.nikolasguillen.questlog.core.common

/**
 * How much of a date [DateUtils] spells out when it renders one for the device locale. The four values
 * mirror the localized date styles every platform offers, so no platform type leaks into shared code.
 */
enum class DateStyle { SHORT, MEDIUM, LONG, FULL }
