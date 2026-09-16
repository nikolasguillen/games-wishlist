package com.example.gameswishlist.core.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the installed version name once at construction, so callers never need to touch [Context] or
 * [PackageManager] themselves.
 *
 * The lookup cannot fail for the app's own package, but the platform declares it as throwing, so a miss
 * degrades to an empty version rather than crashing whatever reads it.
 */
@Singleton
class AppVersionProvider @Inject constructor(@ApplicationContext context: Context) {

    val versionName: String = context.packageManager.versionNameOf(context.packageName)
}

private fun PackageManager.versionNameOf(packageName: String): String = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, 0)
    }.versionName
}.getOrNull().orEmpty()
