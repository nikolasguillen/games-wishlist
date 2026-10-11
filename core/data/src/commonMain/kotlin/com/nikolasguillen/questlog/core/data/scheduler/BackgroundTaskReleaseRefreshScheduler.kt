package com.nikolasguillen.questlog.core.data.scheduler

import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [ReleaseRefreshScheduler] for iOS: the refresh runs in the app at launch and when the saved set changes, and
 * the system also launches it in the background through a `BGAppRefreshTask` ([AppRefreshTaskHandler]).
 *
 * The guarantee is "at most about once per [RELEASE_DATES_REFRESH_INTERVAL], when the system allows": iOS decides
 * if and when a background run happens, so the in-app refresh stays as the fallback. Both paths go through the
 * same [ReleaseRefreshRunner], so they never overlap and never refresh twice.
 *
 * Every run - launch, wishlist change or background - submits the next background request **before** the work
 * starts and again **after** it, whatever the outcome. The first keeps the chain unbroken if the system suspends
 * or kills the app mid-refresh; the second replaces it with the date counted from the new success.
 */
internal class BackgroundTaskReleaseRefreshScheduler(
    private val runner: ReleaseRefreshRunner,
    private val bridge: AppRefreshTaskBridge,
    private val scope: CoroutineScope
) : ReleaseRefreshScheduler, AppRefreshTaskHandler {

    override fun schedulePeriodicRefresh() {
        scope.launch { refreshInApp(onlyIfDue = true) }
    }

    override fun scheduleImmediateRefresh() {
        scope.launch { refreshInApp(onlyIfDue = false) }
    }

    // ATOMIC is delicate because the body runs even if the job was cancelled before it started. That is the point:
    // a run cancelled early must still reach `finally` and report to the system, which requires it.
    @OptIn(DelicateCoroutinesApi::class)
    override fun run(onFinished: (Boolean) -> Unit): AppRefreshTaskRun {
        val job = scope.launch(start = CoroutineStart.ATOMIC) {
            var succeeded = false
            try {
                submitNextRequest()
                succeeded = runner.run(onlyIfDue = true) != ReleaseRefreshOutcome.FAILED
            } finally {
                withContext(NonCancellable) {
                    submitNextRequest()
                    onFinished(succeeded)
                }
            }
        }
        return object : AppRefreshTaskRun {
            override fun cancel() {
                // A finished run must not cancel a later refresh that is unrelated to it.
                if (!job.isActive) return
                job.cancel()
                runner.cancelRunning()
            }
        }
    }

    private suspend fun refreshInApp(onlyIfDue: Boolean) {
        submitNextRequest()
        try {
            runner.run(onlyIfDue)
        } finally {
            withContext(NonCancellable) { submitNextRequest() }
        }
    }

    private suspend fun submitNextRequest() {
        bridge.submitRequest(runner.nextEarliestBegin().toEpochMilliseconds())
    }
}
