package com.example.gameswishlist.core.network

import com.example.gameswishlist.core.network.model.IgdbAuthResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class IgdbAuthManagerTest {

    private var elapsedRealtime = 0L
    private val authService = FakeIgdbAuthService()
    private val manager = IgdbAuthManager(authService) { elapsedRealtime }

    @Test
    fun `a fresh token is reused instead of minted again`() = runTest {
        val first = manager.getAccessToken()
        elapsedRealtime = ONE_HOUR_MILLIS / 2

        val second = manager.getAccessToken()

        assertEquals(first, second)
        assertEquals(1, authService.callCount)
    }

    @Test
    fun `an expired token is replaced`() = runTest {
        val first = manager.getAccessToken()
        elapsedRealtime = ONE_HOUR_MILLIS

        val second = manager.getAccessToken()

        assertEquals("token-1", first)
        assertEquals("token-2", second)
    }

    /**
     * The margin is the whole point of the fix: a token handed out at the very edge of its life would
     * expire while the request carrying it is still in flight.
     */
    @Test
    fun `the token is replaced one minute before it actually expires`() = runTest {
        manager.getAccessToken()
        elapsedRealtime = ONE_HOUR_MILLIS - MARGIN_MILLIS

        val renewed = manager.getAccessToken()

        assertEquals("token-2", renewed)
        assertEquals(2, authService.callCount)
    }

    /**
     * Subtracting the margin from a token that lives less than the margin would leave it already expired,
     * and every single request would then mint a new one. Half the lifetime is used instead.
     */
    @Test
    fun `a token shorter than the margin is still usable`() = runTest {
        authService.expiresInSeconds = 40

        manager.getAccessToken()
        elapsedRealtime = 19_999

        manager.getAccessToken()

        assertEquals(1, authService.callCount)
    }

    @Test
    fun `a token shorter than the margin expires at half its lifetime`() = runTest {
        authService.expiresInSeconds = 40

        manager.getAccessToken()
        elapsedRealtime = 20_000

        manager.getAccessToken()

        assertEquals(2, authService.callCount)
    }

    /**
     * The `Mutex` is the single-flight guard: callers that pile up on a cold cache must not each fire
     * their own request to Twitch.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `parallel callers on a cold cache mint one token between them`() = runTest {
        val inFlight = CompletableDeferred<Unit>()
        authService.beforeReturning = { inFlight.await() }

        val callers = List(5) { async { manager.getAccessToken() } }
        runCurrent()
        inFlight.complete(Unit)
        val tokens = callers.awaitAll()

        assertEquals(1, authService.callCount)
        assertEquals(List(5) { "token-1" }, tokens)
    }

    @Test
    fun `a failed request reports no token`() = runTest {
        authService.failure = IOException("twitch is down")

        assertEquals(null, manager.getAccessToken())
    }

    @Test
    fun `a failure does not poison the cache`() = runTest {
        authService.failure = IOException("twitch is down")
        manager.getAccessToken()

        authService.failure = null

        assertEquals("token-1", manager.getAccessToken())
    }

    /**
     * Cancellation unwinds the calling coroutine; reporting it as a missing token would hide it and send
     * the request out unauthenticated instead.
     */
    @Test
    fun `cancellation is rethrown rather than reported as a missing token`() = runTest {
        val cancellation = CancellationException("scope closed")
        authService.failure = cancellation

        val thrown = runCatching { manager.getAccessToken() }.exceptionOrNull()

        assertSame(cancellation, thrown)
    }

    private class FakeIgdbAuthService : IgdbAuthService {
        var callCount = 0
            private set
        var expiresInSeconds = ONE_HOUR_SECONDS
        var failure: Throwable? = null
        var beforeReturning: suspend () -> Unit = {}

        override suspend fun getAccessToken(
            clientId: String,
            clientSecret: String,
            grantType: String
        ): IgdbAuthResponse {
            failure?.let { throw it }
            callCount++
            beforeReturning()
            return IgdbAuthResponse(
                accessToken = "token-$callCount",
                expiresIn = expiresInSeconds,
                tokenType = "bearer"
            )
        }
    }

    private companion object {
        const val ONE_HOUR_SECONDS = 3_600L
        const val ONE_HOUR_MILLIS = 3_600_000L
        const val MARGIN_MILLIS = 60_000L
    }
}
