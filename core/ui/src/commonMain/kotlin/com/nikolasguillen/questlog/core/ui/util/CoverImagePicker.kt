package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Opens the platform's image picker. [launch] returns at once; the outcome arrives through the `onPicked`
 * passed to [rememberCoverImagePicker].
 */
@Stable
class CoverImagePickerLauncher(private val onLaunch: () -> Unit) {
    fun launch() = onLaunch()
}

/**
 * @param onPicked Called with where the picked image can be read from - a content URI on Android, a path to a
 * temporary file on iOS - or `null` when the picker was dismissed without a choice. The storage layer takes
 * that source string and copies the image into the app's own files.
 */
@Composable
expect fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher
