package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.data.notification.ReminderSyncTriggers
import com.nikolasguillen.questlog.core.ui.util.NotificationAuthorization
import com.nikolasguillen.questlog.core.ui.util.NotificationAuthorizationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification

/**
 * When the iOS reminder coordinator has to resync: each time the app becomes active, and when notification
 * permission goes from not granted to granted.
 *
 * The second one is what arms the first opt-in. The user opts in while the permission is still undecided, so the
 * scheduler's `add` is refused; the reminder must be armed the moment they tap Allow, with no relaunch. Both
 * sources can fire for one change (returning from Settings is an activation and a transition), which is harmless:
 * the coordinator coalesces them.
 */
internal class IosReminderSyncTriggers : ReminderSyncTriggers {

    override val events: Flow<Unit> = merge(appBecameActive(), permissionGranted())

    private fun appBecameActive(): Flow<Unit> = callbackFlow {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) {
            // The permission may have changed while the app was away, and the sync that follows needs it fresh.
            launch {
                NotificationAuthorization.refresh()
                trySend(Unit)
            }
        }
        awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }

    private fun permissionGranted(): Flow<Unit> = flow {
        var previous = NotificationAuthorization.status.value
        NotificationAuthorization.status.collect { current ->
            if (current == NotificationAuthorizationStatus.AUTHORIZED && previous != current) emit(Unit)
            previous = current
        }
    }
}
