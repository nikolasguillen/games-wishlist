package com.nikolasguillen.questlog.core.data.scheduler

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * How often saved games' release dates are refreshed. One value for both platforms: WorkManager's periodic job
 * on Android and the background task on iOS must not drift apart. Neither promises an exact cadence: WorkManager
 * and iOS decide when a run happens, so this is the least time between two successful refreshes, not a schedule.
 */
internal val RELEASE_DATES_REFRESH_INTERVAL: Duration = 24.hours
