package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.ui.window.DialogProperties

// A desktop window has no system bars to lay out around, so the dialog only has to drop the platform's default width.
internal actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
