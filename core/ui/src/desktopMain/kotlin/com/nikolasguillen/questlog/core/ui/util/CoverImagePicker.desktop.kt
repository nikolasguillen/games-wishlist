package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// Stub so the module compiles (specs/020-desktop-app, T023): picking always reports "dismissed". Replaced in T044.
@Composable
actual fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember { CoverImagePickerLauncher { currentOnPicked(null) } }
}
