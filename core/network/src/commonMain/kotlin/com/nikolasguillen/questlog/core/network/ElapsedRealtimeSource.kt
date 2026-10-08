package com.nikolasguillen.questlog.core.network

/**
 * Reads a monotonic "time since boot", the clock `IgdbAuthManager` measures token expiry against.
 *
 * It exists so the manager does not call a platform clock directly, and so a test can drive it by hand:
 * waiting for a real token to expire is not a test anyone can run. Each platform's `networkPlatformModule`
 * binds a clock that keeps counting while the device sleeps (`SystemClock.elapsedRealtime()` on Android).
 */
internal fun interface ElapsedRealtimeSource {
    fun elapsedRealtime(): Long
}
