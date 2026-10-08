package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val IMAGE_TYPE_IDENTIFIER = "public.image"

/**
 * Holds the delegate for as long as the picker can call it: `PHPickerViewController.delegate` is a weak reference,
 * so the object that answers it has to be kept alive from outside.
 */
@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate(private val onPicked: (String?) -> Unit) :
    NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onPicked(null)
            return
        }
        // The provider's URL is deleted as soon as the handler returns, so the file is copied out before then;
        // the storage layer reads it from the copy and removes it.
        result.itemProvider.loadFileRepresentationForTypeIdentifier(IMAGE_TYPE_IDENTIFIER) { url, _ ->
            val copy = url?.let(::copyToTemporaryFile)
            dispatch_async(dispatch_get_main_queue()) { onPicked(copy) }
        }
    }

    private fun copyToTemporaryFile(source: NSURL): String? {
        val extension = source.pathExtension?.takeIf { it.isNotEmpty() } ?: "img"
        val destination = "${NSTemporaryDirectory()}${NSUUID().UUIDString}.$extension"
        val copied = NSFileManager.defaultManager.copyItemAtPath(source.path ?: return null, toPath = destination, error = null)
        return if (copied) destination else null
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCoverImagePicker(onPicked: (source: String?) -> Unit): CoverImagePickerLauncher {
    val latestOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember { PickerDelegate { path -> latestOnPicked(path) } }
    return remember(delegate) {
        CoverImagePickerLauncher {
            val top: UIViewController = topViewController() ?: return@CoverImagePickerLauncher
            val configuration = PHPickerConfiguration().apply {
                selectionLimit = 1
                filter = PHPickerFilter.imagesFilter
            }
            val picker = PHPickerViewController(configuration = configuration)
            picker.delegate = delegate
            top.presentViewController(picker, animated = true, completion = null)
        }
    }
}
