package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// The formats the storage layer decodes with `javax.imageio`.
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp")

/**
 * Opens the operating system's own file dialog, filtered to pictures. The source it reports is the file's absolute
 * path; the storage layer copies the image into the app's data directory and never touches the original.
 *
 * The dialog blocks until it is closed, so it opens off the composition thread (R13), and only one is open at a time.
 */
@Composable
actual fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher {
    val latestOnPicked by rememberUpdatedState(onPicked)
    val scope = rememberCoroutineScope()
    return remember(scope) {
        var open = false
        CoverImagePickerLauncher {
            if (!open) {
                open = true
                scope.launch {
                    val source = try {
                        withContext(Dispatchers.IO) { pickImage() }
                    } finally {
                        open = false
                    }
                    latestOnPicked(source)
                }
            }
        }
    }
}

private fun pickImage(): String? {
    val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
    // macOS honours the filter; Windows' own dialog ignores it, but takes a wildcard pattern as the file name.
    dialog.setFilenameFilter { _, name -> name.hasImageExtension() }
    if (System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)) {
        dialog.file = IMAGE_EXTENSIONS.joinToString(separator = ";") { "*.$it" }
    }
    dialog.isVisible = true
    val picked = dialog.files.firstOrNull() ?: return null
    // The dialog may still have let another type through, so the choice is checked here as well.
    return picked.takeIf { it.name.hasImageExtension() }?.absolutePath
}

private fun String.hasImageExtension(): Boolean = File(this).extension.lowercase() in IMAGE_EXTENSIONS
