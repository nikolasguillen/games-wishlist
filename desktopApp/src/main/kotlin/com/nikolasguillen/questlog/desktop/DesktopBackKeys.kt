package com.nikolasguillen.questlog.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Turns **Escape** and the mouse's **back button** (button 4) into the system back event, which Navigation 3 and the
 * two-pane back handler already listen to through the navigation-event dispatcher (R16, FR-005). The on-screen back
 * arrows keep working; this only adds the keyboard and the mouse.
 *
 * Escape arrives here only after the focused element has passed on it: a menu closes on its own Escape, and a dialog
 * has a window of its own that never reaches this one. Escape with a modifier held is somebody else's shortcut.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DesktopBackKeys(content: @Composable () -> Unit) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current) {
        "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
    }.navigationEventDispatcher
    val input = remember { DirectNavigationEventInput() }

    DisposableEffect(dispatcher, input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }

    Box(
        modifier = Modifier
            .onKeyEvent { event ->
                val isBack = event.type == KeyEventType.KeyDown && event.key == Key.Escape &&
                    !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed && !event.isShiftPressed
                if (isBack) input.backCompleted()
                isBack
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.button == PointerButton.Back) input.backCompleted()
            }
    ) {
        content()
    }
}
