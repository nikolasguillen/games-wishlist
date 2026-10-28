package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.net.http.HttpClient as JdkHttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class GoogleInstalledAppSignInTest {

    private var tokenRequestBody: String? = null

    private fun tokenClient(status: HttpStatusCode = HttpStatusCode.OK, body: String = """{"id_token": "the-id-token"}""") =
        HttpClient(
            MockEngine { request ->
                tokenRequestBody = String(request.body.toByteArray())
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )

    /** A "browser" that opens the redirect the way Google would, with [answer] built from the auth URL's state. */
    private fun browser(answer: (state: String) -> String): (String) -> Unit = { url ->
        val query = URI(url).rawQuery.split('&').associate {
            it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), StandardCharsets.UTF_8)
        }
        val redirect = query.getValue("redirect_uri")
        Thread {
            JdkHttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI("$redirect/?${answer(query.getValue("state"))}")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
        }.start()
    }

    @Test
    fun `the challenge is the sha256 of the verifier without padding`() {
        // The example in RFC 7636, appendix B.
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        )
    }

    @Test
    fun `a verifier is 43 to 128 unreserved characters`() {
        val verifier = randomUrlSafe(32)

        assertTrue(verifier.length in 43..128)
        assertTrue(verifier.all { it.isLetterOrDigit() || it in "-._~" })
        assertFalse(verifier == randomUrlSafe(32))
    }

    @Test
    fun `the authorization url carries what the contract lists`() {
        val url = authorizationUrl("client id", "http://127.0.0.1:5000", "challenge", "state-1")

        assertTrue(url.startsWith("https://accounts.google.com/o/oauth2/v2/auth?"))
        listOf(
            "client_id=client%20id",
            "redirect_uri=http%3A%2F%2F127.0.0.1%3A5000",
            "response_type=code",
            "scope=openid%20email%20profile",
            "code_challenge=challenge",
            "code_challenge_method=S256",
            "state=state-1",
            "prompt=select_account"
        ).forEach { assertTrue(it, url.contains(it)) }
    }

    @Test
    fun `a redirect with the right state gives its code`() {
        assertEquals("4/abc", codeFromRedirect("code=4%2Fabc&state=s&scope=openid", "s"))
    }

    @Test
    fun `a redirect with another state is refused`() {
        try {
            codeFromRedirect("code=abc&state=other", "s")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            // expected
        }
    }

    @Test
    fun `a refusal in the browser is a cancellation`() {
        try {
            codeFromRedirect("error=access_denied&state=s", "s")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.SignInCancelled) {
            // expected
        }
    }

    @Test
    fun `another error from Google is unknown`() {
        try {
            codeFromRedirect("error=server_error&state=s", "s")
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            // expected
        }
    }

    @Test
    fun `the whole flow ends with the id token`() = runBlocking {
        var picked = false
        val signIn = GoogleInstalledAppSignIn(
            httpClient = tokenClient(),
            clientId = "the-client",
            clientSecret = "the-secret",
            openBrowser = browser { state -> "code=the-code&state=$state" }
        )

        val token = signIn.obtainIdToken { picked = true }

        assertEquals("the-id-token", token)
        assertTrue(picked)
        val form = tokenRequestBody!!
        listOf("code=the-code", "client_id=the-client", "client_secret=the-secret", "grant_type=authorization_code", "code_verifier=")
            .forEach { assertTrue(it, form.contains(it)) }
        assertTrue(form.contains("redirect_uri=http%3A%2F%2F127.0.0.1%3A"))
    }

    @Test
    fun `a refusal reaches the caller as a cancellation and no token is asked for`() = runBlocking {
        val signIn = GoogleInstalledAppSignIn(
            httpClient = tokenClient(),
            clientId = "c",
            clientSecret = "s",
            openBrowser = browser { state -> "error=access_denied&state=$state" }
        )

        try {
            signIn.obtainIdToken {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.SignInCancelled) {
            assertEquals(null, tokenRequestBody)
        }
    }

    @Test
    fun `no answer in time is a cancellation`() = runBlocking {
        val signIn = GoogleInstalledAppSignIn(
            httpClient = tokenClient(),
            clientId = "c",
            clientSecret = "s",
            openBrowser = {},
            redirectTimeout = 200.milliseconds
        )

        try {
            signIn.obtainIdToken {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.SignInCancelled) {
            // expected
        }
    }

    @Test
    fun `the server stops when the caller is cancelled`() = runBlocking {
        val opened = AtomicBoolean(false)
        var url: String? = null
        val signIn = GoogleInstalledAppSignIn(
            httpClient = tokenClient(),
            clientId = "c",
            clientSecret = "s",
            openBrowser = { url = it; opened.set(true) }
        )

        val attempt = async { signIn.obtainIdToken {} }
        while (!opened.get()) yield()
        attempt.cancel()
        try {
            attempt.await()
        } catch (_: kotlinx.coroutines.CancellationException) {
            // expected
        }

        val redirect = URI(url!!).rawQuery.split('&').first { it.startsWith("redirect_uri=") }
            .substringAfter('=').let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val refused = try {
            JdkHttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI("$redirect/?code=x&state=y")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
            false
        } catch (_: java.io.IOException) {
            true
        }
        assertTrue("the loopback server must not answer once the sign-in is cancelled", refused)
    }

    @Test
    fun `Google refusing the code is an unknown failure`() = runBlocking {
        val signIn = GoogleInstalledAppSignIn(
            httpClient = tokenClient(HttpStatusCode.BadRequest, """{"error": "invalid_grant"}"""),
            clientId = "c",
            clientSecret = "s",
            openBrowser = browser { state -> "code=bad&state=$state" }
        )

        try {
            signIn.obtainIdToken {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            // expected
        }
    }

    @Test
    fun `a missing client id fails before anything opens`() = runBlocking {
        var opened = false
        val signIn = GoogleInstalledAppSignIn(tokenClient(), clientId = "", clientSecret = "", openBrowser = { opened = true })

        try {
            signIn.obtainIdToken {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            assertFalse(opened)
        }
        assertNotNull(signIn)
    }
}
