package com.nikolasguillen.questlog.shared

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The iOS entry point, called from Swift: the whole app as one view controller.
 *
 * Expects [startQuestLog] to have run, which the app delegate does before any scene is created.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController {
        QuestLogRoot(
            // The only producer of a game deep link is the release reminder, which iOS does not have.
            pendingDeepLinkGameId = null,
            onDeepLinkConsumed = {},
            // iOS exposes no per-corner radius; the content is not clipped to the display.
            displayCornerRadius = 0.dp
        )
    }
