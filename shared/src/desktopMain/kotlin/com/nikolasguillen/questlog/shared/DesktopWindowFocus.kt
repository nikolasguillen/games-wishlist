package com.nikolasguillen.questlog.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The window of the app gaining focus, which `main` reports. Coming back to the app is when the user may have changed
 * the notification permission in the system settings, so the reminders are synced again.
 */
object DesktopWindowFocus {

    private val mutableGained = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val gained: SharedFlow<Unit> = mutableGained.asSharedFlow()

    fun onFocusGained() {
        mutableGained.tryEmit(Unit)
    }
}
