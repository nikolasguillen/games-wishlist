package com.nikolasguillen.questlog.shared

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.nikolasguillen.questlog.core.domain.radar.ReleaseRefreshScheduler
import com.nikolasguillen.questlog.shared.di.initKoin
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * The iOS entry point, called from Swift: the whole app as one view controller.
 *
 * SwiftUI may build the hosting view more than once in a process, so Koin is started only if it is not running
 * yet. The periodic refresh is requested once per launch, the same moment `QuestLogApp.onCreate` does it on
 * Android.
 */
@OptIn(ExperimentalNativeApi::class)
fun MainViewController(): UIViewController {
    if (KoinPlatform.getKoinOrNull() == null) {
        val koin = initKoin(isDebugBuild = Platform.isDebugBinary).koin
        koin.get<ReleaseRefreshScheduler>().schedulePeriodicRefresh()
    }
    return ComposeUIViewController {
        QuestLogRoot(
            // The only producer of a game deep link is the release reminder, which iOS does not have.
            pendingDeepLinkGameId = null,
            onDeepLinkConsumed = {},
            // iOS exposes no per-corner radius; the content is not clipped to the display.
            displayCornerRadius = 0.dp
        )
    }
}
