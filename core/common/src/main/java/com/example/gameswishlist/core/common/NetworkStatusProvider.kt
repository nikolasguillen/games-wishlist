package com.example.gameswishlist.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's current connection type on demand. Unlike [AppVersionProvider], this cannot be
 * read once at construction — it changes while the app runs — so it is exposed as a property computed
 * fresh on every access.
 */
@Singleton
class NetworkStatusProvider @Inject constructor(@ApplicationContext private val context: Context) {

    /** `true` only on an unmetered connection (Wi-Fi or Ethernet) — never on mobile data, metered or not. */
    val isUnmeteredNetworkAvailable: Boolean
        get() {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
}
