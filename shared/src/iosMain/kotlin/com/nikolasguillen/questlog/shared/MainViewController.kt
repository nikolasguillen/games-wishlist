package com.nikolasguillen.questlog.shared

import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import platform.UIKit.UIViewController

/**
 * The iOS entry point, called from Swift: the whole app as one view controller.
 *
 * Expects [startQuestLog] to have run, which the app delegate does before any scene is created.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        val pendingDeepLinkGameId by ReminderDeepLinks.pending.collectAsStateWithLifecycle()
        QuestLogRoot(
            // The game a tapped release reminder asked to open.
            pendingDeepLinkGameId = pendingDeepLinkGameId,
            onDeepLinkConsumed = ReminderDeepLinks::consume,
            // iOS exposes no per-corner radius; the content is not clipped to the display.
            displayCornerRadius = 0.dp
        )
    }
