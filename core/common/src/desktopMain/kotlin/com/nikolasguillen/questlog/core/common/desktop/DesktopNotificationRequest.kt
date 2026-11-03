package com.nikolasguillen.questlog.core.common.desktop

import kotlinx.datetime.LocalDateTime

/** A notification to show: [fireAt] is a wall-clock time in the user's time zone, or `null` to show it now. */
class DesktopNotificationRequest(val gameId: Int, val title: String, val body: String, val fireAt: LocalDateTime?)
