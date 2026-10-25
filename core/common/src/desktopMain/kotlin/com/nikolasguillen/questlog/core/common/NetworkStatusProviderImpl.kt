package com.nikolasguillen.questlog.core.common

/**
 * A desktop has no metered-connection signal the app can read, and the one caller only uses the answer to hold back a
 * large on-device model download, which desktop does not offer. So the connection is always treated as unmetered.
 */
internal class NetworkStatusProviderImpl : NetworkStatusProvider {

    override val isUnmeteredNetworkAvailable: Boolean = true
}
