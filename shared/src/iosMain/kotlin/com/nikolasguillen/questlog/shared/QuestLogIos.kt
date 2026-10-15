package com.nikolasguillen.questlog.shared

import com.nikolasguillen.questlog.core.cloud.IdentityProviderBridge
import com.nikolasguillen.questlog.core.data.notification.ReleaseReminderCoordinator
import com.nikolasguillen.questlog.core.data.notification.ReminderSyncTriggers
import com.nikolasguillen.questlog.core.data.scheduler.AppRefreshTaskBridge
import com.nikolasguillen.questlog.core.data.scheduler.AppRefreshTaskHandler
import com.nikolasguillen.questlog.core.data.scheduler.AppRefreshTaskRun
import com.nikolasguillen.questlog.core.data.translation.AppleLanguageModelBridge
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.core.ui.util.NotificationAuthorization
import com.nikolasguillen.questlog.shared.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.dsl.module
import platform.UserNotifications.UNUserNotificationCenter
import org.koin.mp.KoinPlatform
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

// The notification centre keeps its delegate weakly, so the instance lives here for the whole process.
private val reminderNotificationDelegate = ReleaseReminderNotificationDelegate()

/**
 * Starts the Kotlin side of the app, once per process. Called from the app delegate's `didFinishLaunching`, before
 * it returns.
 *
 * Koin starts here and not in [MainViewController] because the system can launch the app in the background to run
 * the release refresh, and then no scene is connected and no view controller is ever built. The periodic refresh
 * is requested once per launch, the same moment `QuestLogApp.onCreate` does it on Android. It also reads the
 * notification permission, so that it is known before the first screen asks, and starts the reminder coordinator,
 * which brings the pending release reminders in line with the saved games.
 *
 * The notification centre's delegate is set first, before anything that can return early and before this function
 * returns: a tap on a reminder that launched the app is delivered as soon as a delegate exists, and one set later
 * would miss it.
 *
 * Swift supplies the bridges because only Swift can call FoundationModels, BackgroundTasks, GoogleSignIn and
 * AuthenticationServices. A second call does nothing, so the bridges that were bound first stay: they keep no state.
 */
@OptIn(ExperimentalNativeApi::class)
fun startQuestLog(
    languageModelBridge: AppleLanguageModelBridge,
    appRefreshTaskBridge: AppRefreshTaskBridge,
    identityProviderBridge: IdentityProviderBridge
) {
    UNUserNotificationCenter.currentNotificationCenter().delegate = reminderNotificationDelegate
    if (KoinPlatform.getKoinOrNull() != null) return
    val koin = initKoin(isDebugBuild = Platform.isDebugBinary) {
        modules(
            module {
                single<AppleLanguageModelBridge> { languageModelBridge }
                single<AppRefreshTaskBridge> { appRefreshTaskBridge }
                single<IdentityProviderBridge> { identityProviderBridge }
                single<ReminderSyncTriggers> { IosReminderSyncTriggers() }
            }
        )
    }.koin
    koin.get<ReleaseRefreshScheduler>().schedulePeriodicRefresh()
    koin.get<CoroutineScope>().launch { NotificationAuthorization.refresh() }
    koin.get<ReleaseReminderCoordinator>().start()
}

/**
 * The body of the background app-refresh task, called from Swift's launch handler once the system starts it.
 * [onFinished] receives the outcome exactly once; the returned handle ends the run when the system says the time
 * is up. Expects [startQuestLog] to have run, which the app delegate guarantees: the system invokes a launch
 * handler only after launch has finished.
 */
fun runAppRefreshTask(onFinished: (Boolean) -> Unit): AppRefreshTaskRun =
    KoinPlatform.getKoin().get<AppRefreshTaskHandler>().run(onFinished)
