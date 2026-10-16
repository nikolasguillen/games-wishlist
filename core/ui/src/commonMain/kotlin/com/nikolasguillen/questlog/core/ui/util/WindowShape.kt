package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Whether the window is wider than it is tall: the one place the app's "landscape" rule is written.
 *
 * It is the window's shape that decides, not the device's orientation. A narrow, tall split-screen half stays
 * portrait even on a landscape tablet, and a wide tablet window is landscape even when held upright. A square
 * window counts as portrait.
 */
@Composable
fun isLandscapeWindow(): Boolean {
    val size = LocalWindowInfo.current.containerDpSize
    return size.width > size.height
}
