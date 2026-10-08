package com.nikolasguillen.questlog.core.common

/**
 * The installed app's version name, read once, so callers never touch a platform package API themselves.
 * Empty when the platform cannot say.
 */
interface AppVersionProvider {
    val versionName: String
}
