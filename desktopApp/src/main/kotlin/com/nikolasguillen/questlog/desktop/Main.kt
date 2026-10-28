package com.nikolasguillen.questlog.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nikolasguillen.questlog.shared.QuestLogRoot
import com.nikolasguillen.questlog.shared.startQuestLogDesktop

fun main() {
    startQuestLogDesktop(isDebugBuild = true)
    application {
        // The title is the brand name, so it is not a string resource.
        Window(onCloseRequest = ::exitApplication, title = "QuestLog") {
            QuestLogRoot(pendingDeepLinkGameId = null, onDeepLinkConsumed = {})
        }
    }
}
