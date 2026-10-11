package com.nikolasguillen.questlog.core.data.scheduler

/**
 * One background refresh started through [AppRefreshTaskHandler.run], which Swift keeps to end it when the system
 * says the time is up. Public only because Swift has to see it.
 */
interface AppRefreshTaskRun {

    /**
     * Ends the run early. The run's `onFinished` callback still fires, with `false`. Calling it again, or after
     * the run has finished, does nothing.
     */
    fun cancel()
}
