package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/**
 * Whether a space is wider than it is tall: the one place the app's "landscape" rule is written. A square counts as
 * portrait.
 *
 * Use [isLandscapeWindow] for something that is the whole window. Something that lives in a smaller space, such as
 * a pane next to another one, passes the size it was given: a pane of a landscape tablet is portrait-shaped.
 */
fun isLandscapeShape(width: Dp, height: Dp): Boolean = width > height

/**
 * Whether the window is wider than it is tall.
 *
 * It is the window's shape that decides, not the device's orientation. A narrow, tall split-screen half stays
 * portrait even on a landscape tablet, and a wide tablet window is landscape even when held upright.
 */
@Composable
fun isLandscapeWindow(): Boolean {
    val size = LocalWindowInfo.current.containerDpSize
    return isLandscapeShape(size.width, size.height)
}
