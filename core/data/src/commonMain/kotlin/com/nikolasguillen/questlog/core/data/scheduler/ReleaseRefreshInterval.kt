package com.nikolasguillen.questlog.core.data.scheduler

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * How often saved games' release dates are refreshed. One value for both platforms: WorkManager's periodic job
 * on Android and the in-process check on iOS must not drift apart.
 */
internal val RELEASE_DATES_REFRESH_INTERVAL: Duration = 24.hours
