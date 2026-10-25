package com.nikolasguillen.questlog.core.data.scheduler

import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * [ReleaseRefreshScheduler] for desktop: the refresh runs only while the app is open. Nothing is scheduled with the
 * operating system, because a closed app has no process and QuestLog keeps no tray or background service.
 *
 * [schedulePeriodicRefresh] refreshes once at launch and then looks again every [recheckInterval] for as long as
 * [scope] lives. Each look goes through [ReleaseRefreshRunner] with `onlyIfDue = true`, so the runner's own rule
 * (a refresh at most every [RELEASE_DATES_REFRESH_INTERVAL]) decides whether anything happens; the hourly look only
 * means a long-running window does not wait for the next launch. A refresh that moved a date also re-arms its reminder,
 * inside the runner.
 */
internal class InAppReleaseRefreshScheduler(
    private val runner: ReleaseRefreshRunner,
    private val scope: CoroutineScope,
    private val recheckInterval: Duration = 1.hours
) : ReleaseRefreshScheduler {

    private var periodic: Job? = null

    override fun schedulePeriodicRefresh() {
        if (periodic?.isActive == true) return
        periodic = scope.launch {
            while (true) {
                refresh(onlyIfDue = true)
                delay(recheckInterval)
            }
        }
    }

    override fun scheduleImmediateRefresh() {
        scope.launch { refresh(onlyIfDue = false) }
    }

    // A failed refresh is not reported: the next look tries again, and the user never asked for this one.
    private suspend fun refresh(onlyIfDue: Boolean) {
        try {
            runner.run(onlyIfDue)
        } catch (_: CancellationException) {
            // The runner also throws this when its own refresh was cancelled; only a cancelled caller must stop.
            currentCoroutineContext().ensureActive()
        } catch (_: Exception) {
        }
    }
}
