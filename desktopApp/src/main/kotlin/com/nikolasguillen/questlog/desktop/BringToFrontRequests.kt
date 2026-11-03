package com.nikolasguillen.questlog.desktop

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Asks the window to come to the front, from code that has no window: a click on a notification arrives on a thread of
 * the operating system's choosing, possibly before the window exists. The window collects [requests] once it is open.
 */
internal object BringToFrontRequests {

    private val mutableRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val requests: SharedFlow<Unit> = mutableRequests.asSharedFlow()

    fun request() {
        mutableRequests.tryEmit(Unit)
    }
}
