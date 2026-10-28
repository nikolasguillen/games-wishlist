package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.shared.di.initKoin
import org.koin.core.Koin
import org.koin.mp.KoinPlatform

/**
 * Starts the Kotlin side of the desktop app, once per process, and returns its [Koin] instance. A second call returns
 * the running instance and does nothing else.
 *
 * `main` calls this after it has installed the notification click handler and decided that it is the only running
 * instance, and before it opens the window. The periodic release refresh is requested once per launch, the same moment
 * `QuestLogApp.onCreate` does it on Android; it runs only while the app is open.
 */
fun startQuestLogDesktop(isDebugBuild: Boolean): Koin {
    KoinPlatform.getKoinOrNull()?.let { return it }
    val koin = initKoin(isDebugBuild).koin
    koin.get<ReleaseRefreshScheduler>().schedulePeriodicRefresh()
    return koin
}
