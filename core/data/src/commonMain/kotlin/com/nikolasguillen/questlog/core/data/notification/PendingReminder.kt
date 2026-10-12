package com.nikolasguillen.questlog.core.data.notification

import kotlin.time.Instant

/** A reminder the notification centre still has to deliver. [nextTriggerAt] is `null` for an immediate trigger. */
internal data class PendingReminder(val gameId: Int, val nextTriggerAt: Instant?)
