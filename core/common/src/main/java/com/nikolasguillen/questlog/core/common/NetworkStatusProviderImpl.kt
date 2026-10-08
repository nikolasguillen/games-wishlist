package com.nikolasguillen.questlog.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
internal class NetworkStatusProviderImpl(
    private val context: Context
) : NetworkStatusProvider {

    override val isUnmeteredNetworkAvailable: Boolean
        get() {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
}
