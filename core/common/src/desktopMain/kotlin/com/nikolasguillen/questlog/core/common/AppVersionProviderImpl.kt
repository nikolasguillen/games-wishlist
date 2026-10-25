package com.nikolasguillen.questlog.core.common

/**
 * Reads the version name from the `questlog.version` system property, which the desktop launcher sets from the
 * packaged version. A run without the property degrades to an empty version rather than failing whatever reads it,
 * as on Android and iOS.
 */
internal class AppVersionProviderImpl : AppVersionProvider {

    override val versionName: String = System.getProperty("questlog.version").orEmpty()
}
