package com.nikolasguillen.questlog.core.data.notification

import kotlinx.datetime.LocalDateTime

/**
 * One local notification to hand to the platform's notification centre.
 *
 * [fireAt] is `null` for "fire in one second", which is how a reminder that is already due is delivered. Otherwise
 * it is calendar components with no time zone attached ("floating"), so the system fires at that wall-clock time
 * wherever the device is, and the 09:00 of a release day stays 09:00 after the user travels.
 */
internal data class LocalReminderRequest(
    val gameId: Int,
    val title: String,
    val body: String,
    val fireAt: LocalDateTime?
)
