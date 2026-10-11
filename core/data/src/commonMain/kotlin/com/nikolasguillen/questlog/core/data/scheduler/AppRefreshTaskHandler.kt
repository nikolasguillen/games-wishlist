package com.nikolasguillen.questlog.core.data.scheduler

/**
 * The body of the iOS background app-refresh task, as Swift calls it when the system launches the task.
 *
 * Implemented in Kotlin so the scheduling rules stay in shared code; Swift only registers the task, passes the
 * result to the system and forwards expiration. Public only because Swift has to see it.
 *
 * Nothing here is `suspend`, for the reasons recorded on [AppRefreshTaskBridge].
 */
interface AppRefreshTaskHandler {

    /**
     * Runs one background refresh: submits the next request, refreshes if one is due (joining a refresh already
     * in flight), and submits again.
     *
     * [onFinished] is called exactly once, from any thread, after the next request has been submitted: `true` if
     * the refresh succeeded or was not due, `false` if it failed or was cancelled.
     */
    fun run(onFinished: (Boolean) -> Unit): AppRefreshTaskRun
}
