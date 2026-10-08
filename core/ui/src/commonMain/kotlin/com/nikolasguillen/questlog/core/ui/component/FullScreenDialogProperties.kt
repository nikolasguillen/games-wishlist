package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.ui.window.DialogProperties

/**
 * A dialog that fills the screen edge to edge and lays itself out around the system bars. How a dialog opts
 * out of the platform's own insets is a platform option, so each platform builds it.
 */
internal expect fun fullScreenDialogProperties(): DialogProperties
