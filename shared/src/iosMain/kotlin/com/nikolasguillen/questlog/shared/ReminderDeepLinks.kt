package com.nikolasguillen.questlog.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The game a tapped release notification asked to open, handed to `QuestLogRoot` the way `MainActivity` hands over
 * its intent's game id. Process-wide because the tap can be delivered before any scene exists, when the app is
 * launched by it: the value waits here until the root composes and consumes it.
 */
internal object ReminderDeepLinks {
    private val mutablePending = MutableStateFlow<Int?>(null)

    val pending: StateFlow<Int?> = mutablePending.asStateFlow()

    fun emit(gameId: Int) {
        mutablePending.value = gameId
    }

    fun consume() {
        mutablePending.value = null
    }
}
