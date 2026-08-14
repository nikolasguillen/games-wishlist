package com.example.gameswishlist.core.network

import android.os.SystemClock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory cache of the Twitch client-credentials token used to call IGDB.
 *
 * Nothing is persisted: every process starts without a token and fetches one on its first request.
 * The token is then reused until it is close to expiring, or until IGDB rejects it.
 */
@Singleton
class IgdbAuthManager @Inject constructor(
    private val authService: IgdbAuthService
) {
    private val mutex = Mutex()

    private var accessToken: String? = null

    /** [SystemClock.elapsedRealtime] reading past which [accessToken] must not be sent any more. */
    private var expiresAtMillis: Long = 0L

    /**
     * Returns a token to authenticate a request with, fetching a new one when the cached token is
     * missing or about to expire.
     *
     * @throws IOException if the token request fails.
     */
    suspend fun getAccessToken(): String = mutex.withLock {
        accessToken?.takeIf { SystemClock.elapsedRealtime() < expiresAtMillis } ?: fetchToken()
    }

    /**
     * Discards [staleToken] after IGDB rejected it and returns the token to retry the request with.
     *
     * Every in-flight request fails with the same stale token, so only the first caller through the
     * mutex refetches; the others find a token that is no longer the one they sent and reuse it.
     *
     * @throws IOException if the token request fails.
     */
    suspend fun refreshAccessToken(staleToken: String): String = mutex.withLock {
        accessToken?.takeUnless { it == staleToken } ?: fetchToken()
    }

    /**
     * Both callers are OkHttp components, which are only allowed to fail with an [IOException] —
     * anything else escapes on the dispatcher thread instead of reaching the caller of the request.
     */
    private suspend fun fetchToken(): String {
        val response = try {
            authService.getAccessToken(
                clientId = BuildConfig.IGDB_CLIENT_ID,
                clientSecret = BuildConfig.IGDB_CLIENT_SECRET
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("IGDB authentication failed", e)
        }

        // Expire the cache slightly early: a token that passes the check here still has to survive the
        // round trip to IGDB.
        val lifetimeSeconds = (response.expiresIn - EXPIRY_MARGIN_SECONDS).coerceAtLeast(0)
        accessToken = response.accessToken
        expiresAtMillis = SystemClock.elapsedRealtime() + lifetimeSeconds * MILLIS_PER_SECOND

        return response.accessToken
    }

    private companion object {
        const val EXPIRY_MARGIN_SECONDS = 60L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
