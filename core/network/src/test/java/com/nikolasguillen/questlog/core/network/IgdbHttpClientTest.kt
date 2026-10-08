package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbAuthResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.ktor.client.network.sockets.SocketTimeoutException as KtorSocketTimeoutException

/**
 * Covers the HTTP client end to end against a scripted engine: what goes out (URL, method, body, credentials)
 * and what comes back (decoded rows, or this module's own exceptions), without touching the network.
 */
class IgdbHttpClientTest {

    private val gamesJson = """[ { "id": 1, "name": "A" }, { "id": 2, "name": "B", "summary": "s" } ]"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private class FakeAuthService(private val fail: Boolean = false) : IgdbAuthService {
        override suspend fun getAccessToken(clientId: String, clientSecret: String, grantType: String): IgdbAuthResponse {
            if (fail) throw IllegalStateException("no token")
            return IgdbAuthResponse(accessToken = "tok", expiresIn = 3_600, tokenType = "bearer")
        }
    }

    private fun serviceOver(
        engine: MockEngine,
        authService: IgdbAuthService = FakeAuthService(),
        logBodies: Boolean = false
    ): IgdbApiService = IgdbApiServiceImpl(
        createIgdbHttpClient(
            engine = engine,
            authManager = IgdbAuthManager(authService) { 0L },
            clientId = "client-123",
            logBodies = logBodies
        )
    )

    private fun MockRequestHandleScope.json(content: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond(content, status, jsonHeaders)

    private val HttpRequestData.text: String get() = (body as TextContent).text

    /** Runs [block], which must throw a [T], and returns it. A suspend-friendly stand-in for `assertThrows`. */
    private suspend inline fun <reified T : Throwable> thrownBy(block: () -> Unit): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw e
        }
        throw AssertionError("expected ${T::class.simpleName} to be thrown")
    }

    @Test
    fun `a query is posted as plain text to the endpoint's path and the rows come back decoded`() = runTest {
        val engine = MockEngine { json(gamesJson) }

        val games = serviceOver(engine).searchGames("fields name; limit 2;")

        assertEquals(listOf(1, 2), games.map { it.id })
        assertEquals("s", games[1].summary)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://api.igdb.com/v4/games", request.url.toString())
        assertEquals("fields name; limit 2;", request.text)
        assertEquals(ContentType.Text.Plain.contentType, (request.body as TextContent).contentType.contentType)
        assertEquals(ContentType.Text.Plain.contentSubtype, (request.body as TextContent).contentType.contentSubtype)
    }

    @Test
    fun `each method reaches its own IGDB path`() = runTest {
        val engine = MockEngine { json("[]") }
        val service = serviceOver(engine)

        service.searchGames("q")
        service.getGameDetail("q")
        service.getPopularityPrimitives("q")
        service.getPlatforms("q")
        service.getReleaseDates("q")

        assertEquals(
            listOf("games", "games", "popularity_primitives", "platforms", "release_dates"),
            engine.requestHistory.map { it.url.encodedPath.removePrefix("/v4/") }
        )
    }

    @Test
    fun `every request carries the client id and the bearer token`() = runTest {
        val engine = MockEngine { json("[]") }

        serviceOver(engine).searchGames("q")

        val headers = engine.requestHistory.single().headers
        assertEquals("client-123", headers["Client-ID"])
        assertEquals("Bearer tok", headers[HttpHeaders.Authorization])
    }

    /**
     * Fetching the token failed, so the request goes out without it and comes back as a 401, which `:core:data`
     * - the error boundary - turns into a typed error.
     */
    @Test
    fun `a request still goes out, unauthorised, when no token could be fetched`() = runTest {
        val engine = MockEngine { json("[]") }

        serviceOver(engine, authService = FakeAuthService(fail = true)).searchGames("q")

        val headers = engine.requestHistory.single().headers
        assertEquals("client-123", headers["Client-ID"])
        assertNull(headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a non-2xx status becomes an IgdbHttpException carrying the code`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }

        val thrown = thrownBy<IgdbHttpException> { serviceOver(engine).searchGames("q") }

        assertEquals(404, thrown.code)
        assertEquals("HTTP 404 Not Found", thrown.message)
    }

    @Test
    fun `rate limiting is reported with its own code`() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.TooManyRequests) }

        val thrown = thrownBy<IgdbHttpException> { serviceOver(engine).getPlatforms("q") }

        assertEquals(429, thrown.code)
    }

    @Test
    fun `the client's timeouts are reported as IgdbTimeoutException`() = runTest {
        val timeouts = listOf<Throwable>(
            HttpRequestTimeoutException("https://api.igdb.com/v4/games", 1_000),
            ConnectTimeoutException("connect"),
            KtorSocketTimeoutException("socket")
        )

        timeouts.forEach { timeout ->
            val engine = MockEngine { throw timeout }

            val thrown = thrownBy<IgdbTimeoutException> { serviceOver(engine).searchGames("q") }

            // The client recovers the stack trace by re-creating the exception, so the cause is an equal
            // copy of the original rather than the same instance.
            assertEquals(timeout::class, thrown.cause?.let { it::class })
            assertEquals(timeout.message, thrown.cause?.message)
        }
    }

    @Test
    fun `cancellation passes through untouched`() = runTest {
        val cancellation = CancellationException("scope closed")
        val engine = MockEngine { throw cancellation }

        val thrown = thrownBy<CancellationException> { serviceOver(engine).searchGames("q") }

        assertTrue(thrown === cancellation || thrown.cause === cancellation)
    }

    @Test
    fun `the token call posts the credentials as query parameters and decodes the token`() = runTest {
        val engine = MockEngine {
            json("""{ "access_token": "abc", "expires_in": 5184000, "token_type": "bearer" }""")
        }

        val response = IgdbAuthServiceImpl(createAuthHttpClient(engine)).getAccessToken("id", "secret")

        assertEquals("abc", response.accessToken)
        assertEquals(5_184_000L, response.expiresIn)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://id.twitch.tv/oauth2/token", request.url.toString().substringBefore('?'))
        assertEquals("id", request.url.parameters["client_id"])
        assertEquals("secret", request.url.parameters["client_secret"])
        assertEquals("client_credentials", request.url.parameters["grant_type"])
    }

    @Test
    fun `a rejected token call fails with the status code, not a decoding error`() = runTest {
        val engine = MockEngine { json("""{ "status": 400, "message": "invalid client" }""", HttpStatusCode.BadRequest) }

        val thrown = thrownBy<IgdbHttpException> {
                IgdbAuthServiceImpl(createAuthHttpClient(engine)).getAccessToken("id", "wrong")
            }

        assertEquals(400, thrown.code)
    }
}
