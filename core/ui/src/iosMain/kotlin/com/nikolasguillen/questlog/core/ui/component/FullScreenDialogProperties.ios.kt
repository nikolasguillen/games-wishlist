package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.ui.window.DialogProperties

internal actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    usePlatformInsets = false
)
