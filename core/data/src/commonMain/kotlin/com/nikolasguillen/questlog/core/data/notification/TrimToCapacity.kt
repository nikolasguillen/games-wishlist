package com.nikolasguillen.questlog.core.data.notification

import kotlin.time.Instant

/** iOS keeps at most 64 pending local notifications per app and silently drops the rest. */
internal const val MAX_PENDING_REMINDERS = 64

/**
 * The ids to remove so that only the [capacity] soonest reminders stay pending. An immediate trigger (`null`)
 * counts as the soonest, and equal times are ordered by game id so the choice is stable between syncs.
 */
internal fun trimToCapacity(pending: List<PendingReminder>, capacity: Int): List<Int> =
    pending
        .sortedWith(compareBy<PendingReminder, Instant?>(nullsFirst()) { it.nextTriggerAt }.thenBy { it.gameId })
        .drop(capacity)
        .map { it.gameId }
