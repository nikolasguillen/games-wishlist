package com.nikolasguillen.questlog.core.data.scheduler

import kotlin.time.Instant

/** Records every request the scheduler submits, in order. */
internal class FakeAppRefreshTaskBridge : AppRefreshTaskBridge {

    val submitted = mutableListOf<Instant>()

    override fun submitRequest(earliestBeginEpochMillis: Long) {
        submitted += Instant.fromEpochMilliseconds(earliestBeginEpochMillis)
    }
}
