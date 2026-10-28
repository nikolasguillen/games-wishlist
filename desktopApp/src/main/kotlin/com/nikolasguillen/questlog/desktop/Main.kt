package com.nikolasguillen.questlog.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikolasguillen.questlog.shared.QuestLogRoot
import com.nikolasguillen.questlog.shared.consumeReminderDeepLink
import com.nikolasguillen.questlog.shared.emitReminderDeepLink
import com.nikolasguillen.questlog.shared.pendingReminderDeepLink
import com.nikolasguillen.questlog.shared.startQuestLogDesktop
import java.awt.Dimension
import kotlin.system.exitProcess

// The narrow layout is the smallest the screens are built for.
private val MINIMUM_WINDOW_SIZE = Dimension(400, 640)

fun main() {
    // One window per user: a second launch tells the first to come forward and ends here.
    val instance = SingleInstance.acquireOrHandOver(handOver = SingleInstanceMessage.Focus) ?: exitProcess(0)

    startQuestLogDesktop(isDebugBuild = true)
    installDockIcon()

    application {
        Window(
            title = "QuestLog", // The brand name, so it is not a string resource.
            icon = appIconPainter,
            state = rememberWindowState(size = DpSize(1100.dp, 760.dp)),
            onCloseRequest = ::exitApplication
        ) {
            window.minimumSize = MINIMUM_WINDOW_SIZE

            LaunchedEffect(instance) {
                instance.messages.collect { message ->
                    if (message is SingleInstanceMessage.OpenGame) emitReminderDeepLink(message.gameId)
                    bringToFront()
                }
            }

            DesktopBackKeys {
                val pendingDeepLinkGameId by pendingReminderDeepLink.collectAsStateWithLifecycle()
                QuestLogRoot(
                    pendingDeepLinkGameId = pendingDeepLinkGameId,
                    onDeepLinkConsumed = ::consumeReminderDeepLink
                )
            }
        }
    }

    // Closing the window quits: no tray icon and no background service stay behind. The process is ended outright
    // because the HTTP client's idle threads are not daemons and would keep it alive for a while.
    instance.close()
    exitProcess(0)
}

private fun FrameWindowScope.bringToFront() {
    window.isMinimized = false
    window.toFront()
    window.requestFocus()
}
