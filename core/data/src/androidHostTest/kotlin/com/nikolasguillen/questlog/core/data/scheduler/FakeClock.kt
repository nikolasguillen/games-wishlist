package com.nikolasguillen.questlog.core.data.scheduler

import kotlin.time.Clock
import kotlin.time.Instant

internal class FakeClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}
