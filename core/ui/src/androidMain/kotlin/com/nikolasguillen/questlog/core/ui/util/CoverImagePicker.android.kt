package com.nikolasguillen.questlog.core.ui.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher {
    val latestOnPicked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> latestOnPicked(uri?.toString()) }
    return remember(launcher) {
        CoverImagePickerLauncher {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
