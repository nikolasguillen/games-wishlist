package com.nikolasguillen.questlog.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Sends system back to [onBack] ahead of every ordinary back handler in the composition.
 *
 * `NavigationBackHandler` registers at the default priority, where the handler added last wins, and that order is not
 * something a scene controls: the Search pane's own handler (it clears the query) can end up registered after the
 * scene's. This one registers at the overlay priority, which is always asked first, so closing the game opened beside
 * Search is what back does while it is showing.
 *
 * It sees the gesture only when it completes, so there is no predictive back preview while two panes are shown.
 */
@Composable
internal fun SearchDetailBackHandler(onBack: () -> Unit) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current) {
        "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
    }.navigationEventDispatcher
    val currentOnBack by rememberUpdatedState(onBack)

    DisposableEffect(dispatcher) {
        val handler = object : NavigationEventHandler<NavigationEventInfo>(
            initialInfo = NavigationEventInfo.None,
            isBackEnabled = true
        ) {
            override fun onBackCompleted() = currentOnBack()
        }
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { handler.remove() }
    }
}
