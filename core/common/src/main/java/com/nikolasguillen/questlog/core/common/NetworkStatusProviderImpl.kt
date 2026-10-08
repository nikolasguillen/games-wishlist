package com.nikolasguillen.questlog.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NetworkStatusProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkStatusProvider {

    override val isUnmeteredNetworkAvailable: Boolean
        get() {
            val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }
}
