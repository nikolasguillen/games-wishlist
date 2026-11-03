package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.common.desktop.DesktopNotificationStatus
import com.nikolasguillen.questlog.core.data.notification.ReminderSyncTriggers
import com.nikolasguillen.questlog.core.ui.util.DesktopNotificationAuthorization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * When the desktop reminder coordinator has to resync: each time the window gains focus, and when notification
 * permission goes from not granted to granted. Like iOS, the second one arms a first opt-in made while the permission
 * was still undecided, with no relaunch. Both can fire for one change, which is harmless: the coordinator coalesces
 * them.
 */
internal class DesktopReminderSyncTriggers : ReminderSyncTriggers {

    override val events: Flow<Unit> = merge(windowGainedFocus(), permissionGranted())

    private fun windowGainedFocus(): Flow<Unit> = DesktopWindowFocus.gained.map {
        // The permission may have changed while the app was in the background, and the sync that follows needs it fresh.
        DesktopNotificationAuthorization.refresh()
    }

    private fun permissionGranted(): Flow<Unit> = flow {
        var previous = DesktopNotificationAuthorization.status.value
        DesktopNotificationAuthorization.status.collect { current ->
            if (current == DesktopNotificationStatus.AUTHORIZED && previous != current) emit(Unit)
            previous = current
        }
    }
}
