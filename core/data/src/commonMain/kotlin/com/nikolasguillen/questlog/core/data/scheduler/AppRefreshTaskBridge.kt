package com.nikolasguillen.questlog.core.data.scheduler

/**
 * The iOS background-task service, as the release refresh sees it.
 *
 * Implemented in Swift by `iosApp` (`ReleaseRefreshTask`) and handed to Kotlin through `startQuestLog`: the
 * BackgroundTasks framework is called from Swift, which owns the task identifier. Public only because Swift has
 * to see it to implement it.
 *
 * Nothing here is `suspend`: a `suspend` member implemented in Swift has no cancellation and crashes on a thrown
 * Swift error, so a failed submit is logged and swallowed on the Swift side.
 */
interface AppRefreshTaskBridge {

    /**
     * Replaces any pending request with one that may start no earlier than [earliestBeginEpochMillis]. The system
     * decides when, and whether, it actually runs. May be called from any thread.
     */
    fun submitRequest(earliestBeginEpochMillis: Long)
}
