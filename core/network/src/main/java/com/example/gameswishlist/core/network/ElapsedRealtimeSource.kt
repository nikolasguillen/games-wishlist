package com.example.gameswishlist.core.network

/**
 * Reads a monotonic "time since boot", the clock `IgdbAuthManager` measures token expiry against.
 *
 * It exists so the manager does not call `android.os.SystemClock` directly: that method is stubbed out in
 * JVM unit tests, and waiting for a real token to expire is not a test anyone can run. The production
 * binding in `NetworkModule` is `SystemClock.elapsedRealtime()`; tests drive it by hand.
 */
internal fun interface ElapsedRealtimeSource {
    fun elapsedRealtime(): Long
}
