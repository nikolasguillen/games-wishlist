package com.nikolasguillen.questlog.core.data.notification

import kotlinx.coroutines.flow.Flow

/**
 * What tells the iOS [ReleaseReminderCoordinator] to resync: the app became active, or notification permission
 * was newly granted. Implemented in `:shared`, because the permission state it listens to lives in `:core:ui`.
 */
interface ReminderSyncTriggers {
    val events: Flow<Unit>
}
