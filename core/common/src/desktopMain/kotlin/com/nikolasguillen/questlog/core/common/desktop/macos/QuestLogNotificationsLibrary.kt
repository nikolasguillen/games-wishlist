package com.nikolasguillen.questlog.core.common.desktop.macos

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Pointer

/** The C ABI of `libQuestLogNotifications.dylib` (desktopApp/native/macos), exactly as the Swift side exports it. */
@Suppress("FunctionName") // The names are the C symbols.
internal interface QuestLogNotificationsLibrary : Library {

    /** `0` not determined, `1` authorized, `2` denied, `-1` not running from an app bundle. */
    fun ql_authorization_status(): Int

    fun ql_request_authorization(done: ResultCallback)

    fun ql_add_calendar(
        id: Int, title: String, body: String,
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        done: ResultCallback
    )

    fun ql_add_immediate(id: Int, title: String, body: String, done: ResultCallback)

    fun ql_pending(done: PendingCallback)

    fun ql_remove(ids: IntArray, count: Int)

    fun ql_set_click_handler(onClick: ClickCallback)

    /** `1` on success. Called exactly once, on any thread. */
    fun interface ResultCallback : Callback {
        fun invoke(value: Int)
    }

    /** The arrays are valid only until the call returns. A `NaN` time is a reminder that is shown at once. */
    fun interface PendingCallback : Callback {
        fun invoke(count: Int, ids: Pointer?, epochSeconds: Pointer?)
    }

    fun interface ClickCallback : Callback {
        fun invoke(gameId: Int)
    }
}
