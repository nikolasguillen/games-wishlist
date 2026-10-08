package com.nikolasguillen.questlog.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * The app's private, backed-up storage on iOS (`Library/Application Support`), created if it does not exist yet.
 * The database, the settings file and the cover images live here, the counterpart of Android's internal files
 * directory.
 */
@OptIn(ExperimentalForeignApi::class)
fun applicationSupportDirectory(): String {
    val url: NSURL = checkNotNull(
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
    ) { "Application Support directory is unavailable" }
    return checkNotNull(url.path) { "Application Support directory has no path" }
}
