package com.example.gameswishlist.core.network

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caches the Twitch client-credentials token used to call IGDB and mints a new one once it expires.
 *
 * The token is kept in memory only. It is cheap to request again on the next process start, and writing it
 * to disk would mean persisting a credential the app can always re-derive.
 */
@Singleton
class IgdbAuthManager @Inject constructor(
    private val authService: IgdbAuthService
) {
    private val mutex = Mutex()
    private var accessToken: String? = null

    /**
     * Deadline for [accessToken] on [SystemClock.elapsedRealtime]'s timeline. That clock is monotonic and
     * keeps counting while the device sleeps, unlike the wall clock — which the user or the network can
     * move backwards, stranding an expired token in the cache, or forwards, discarding a valid one.
     */
    private var expiresAtMillis: Long = 0L

    /** Returns a usable token, requesting a new one when the cache is empty or about to expire. */
    suspend fun getAccessToken(): String? = mutex.withLock {
        accessToken?.takeIf { isFresh() } ?: fetchToken()
    }

    private fun isFresh(): Boolean = SystemClock.elapsedRealtime() < expiresAtMillis

    private suspend fun fetchToken(): String? = try {
        val response = authService.getAccessToken(
            clientId = BuildConfig.IGDB_CLIENT_ID,
            clientSecret = BuildConfig.IGDB_CLIENT_SECRET
        )
        accessToken = response.accessToken
        val lifetimeMillis = response.expiresIn.coerceAtLeast(0).seconds.inWholeMilliseconds
        // Twitch hands out tokens that live for weeks, so the margin is a rounding error. The floor at
        // half the lifetime is there for the degenerate answer: without it a short-lived token would be
        // born already expired and every single request would mint a new one.
        val usableMillis = (lifetimeMillis - EXPIRY_MARGIN.inWholeMilliseconds)
            .coerceAtLeast(lifetimeMillis / 2)
        expiresAtMillis = SystemClock.elapsedRealtime() + usableMillis
        accessToken
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Deliberate: this runs inside an OkHttp interceptor, so throwing here would surface as a raw
        // failure from the wrong layer. Reporting "no token" instead lets the request go out unauthorized
        // and come back a 401, which :core:data — the error boundary — already maps to a typed
        // RepositoryError. Do not turn this into a rethrow without moving that boundary.
        clearToken()
        null
    }

    private fun clearToken() {
        accessToken = null
        expiresAtMillis = 0L
    }

    private companion object {
        /**
         * Mint the next token slightly early, so one cannot expire in the window between this check and
         * the request reaching IGDB.
         */
        val EXPIRY_MARGIN = 60.seconds
    }
}
