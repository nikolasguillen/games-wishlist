package com.nikolasguillen.questlog.core.common

/**
 * Reads the device's current connection type on demand. Unlike [AppVersionProvider], this cannot be
 * read once at construction — it changes while the app runs — so it is exposed as a property computed
 * fresh on every access.
 */
interface NetworkStatusProvider {

    /** `true` only on an unmetered connection (Wi-Fi or Ethernet) — never on mobile data, metered or not. */
    val isUnmeteredNetworkAvailable: Boolean
}
