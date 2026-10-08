package com.nikolasguillen.questlog.core.common

import platform.Foundation.NSBundle

/**
 * Reads the version name from the app bundle once at construction. A missing key degrades to an empty
 * version rather than failing whatever reads it, as on Android.
 */
internal class AppVersionProviderImpl : AppVersionProvider {

    override val versionName: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String).orEmpty()
}
