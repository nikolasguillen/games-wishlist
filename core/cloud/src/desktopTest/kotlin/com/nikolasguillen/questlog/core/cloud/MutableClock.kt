package com.nikolasguillen.questlog.core.cloud

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A clock a test moves by hand. */
internal class MutableClock(private var now: Instant = Instant.fromEpochSeconds(1_700_000_000)) : Clock {

    override fun now(): Instant = now

    fun advanceSeconds(seconds: Long) {
        now += seconds.seconds
    }
}
