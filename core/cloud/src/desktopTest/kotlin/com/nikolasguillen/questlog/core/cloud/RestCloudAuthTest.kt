package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class RestCloudAuthTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val requests = mutableListOf<HttpRequestData>()
    private val bodies = mutableListOf<String>()

    /** What each Firebase endpoint answers; a test replaces the one it is about. */
    private var signInAnswer = ok(
        """{"localId": "uid-1", "email": "player@example.com", "idToken": "id-1", "refreshToken": "refresh-1", "expiresIn": "3600"}"""
    )
    private var lookupAnswer = ok(
        """{"users": [{"email": "player@example.com", "providerUserInfo": [{"providerId": "google.com"}, {"providerId": "apple.com"}]}]}"""
    )
    private var refreshAnswer = ok(
        """{"user_id": "uid-1", "id_token": "id-2", "refresh_token": "refresh-2", "expires_in": "3600"}"""
    )
    private var deleteAnswer = ok("{}")

    private var googleSignIns = 0
    private var picked = 0

    private val google = GoogleIdTokenSource { onAccountPicked ->
        googleSignIns++
        onAccountPicked()
        "google-id-token"
    }

    private fun ok(body: String) = HttpStatusCode.OK to body

    private fun failure(message: String) = HttpStatusCode.BadRequest to """{"error": {"code": 400, "message": "$message"}}"""

    private fun count(endpoint: String) = requests.count { it.url.encodedPath.contains(endpoint) }

    private val client = HttpClient(
        MockEngine { request ->
            requests += request
            bodies += String(request.body.toByteArray())
            val path = request.url.encodedPath
            val (status, body) = when {
                path.endsWith("signInWithIdp") -> signInAnswer
                path.endsWith("lookup") -> lookupAnswer
                path.endsWith("delete") -> deleteAnswer
                path.endsWith("/token") -> refreshAnswer
                else -> HttpStatusCode.NotFound to "{}"
            }
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    )

    private fun store() = DesktopCloudSessionStore(folder.root.toPath())

    private fun auth(clock: MutableClock = MutableClock()) =
        RestCloudAuth(IdentityToolkitClient(client, apiKey = "api-key"), google, store(), clock)

    @Test
    fun `signing in stores the session and shows the user`() = runBlocking {
        val auth = auth()

        val user = auth.signIn { picked++ }

        assertEquals(CloudUser("uid-1", "player@example.com", setOf(CloudProvider.GOOGLE, CloudProvider.APPLE)), user)
        assertEquals(user, auth.currentUser.first())
        assertEquals("refresh-1", store().load()!!.refreshToken)
        assertEquals(1, picked)
        assertTrue(bodies.any { it.contains("id_token=google-id-token&providerId=google.com") })
    }

    @Test
    fun `a stored session is the user at start`() = runBlocking {
        store().save(DesktopCloudSession("uid-9", "a@b.c", listOf("google.com"), "r"))

        assertEquals(CloudUser("uid-9", "a@b.c", setOf(CloudProvider.GOOGLE)), auth().currentUser.first())
    }

    @Test
    fun `nobody is signed in without a session`() = runBlocking {
        assertNull(auth().currentUser.first())
    }

    @Test
    fun `an unconfirmed credential is a collision and nothing is stored`() = runBlocking {
        signInAnswer = ok("""{"needConfirmation": true, "email": "player@example.com"}""")

        try {
            auth().signIn {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.CredentialCollision) {
            assertNull(store().load())
        }
    }

    @Test
    fun `a lookup that fails does not undo the sign in`() = runBlocking {
        lookupAnswer = failure("INVALID_ID_TOKEN")

        val user = auth().signIn {}

        assertEquals(setOf(CloudProvider.GOOGLE), user.providers)
    }

    @Test
    fun `a token near expiry is refreshed once and then reused`() = runBlocking {
        signInAnswer = ok("""{"localId": "uid-1", "idToken": "id-1", "refreshToken": "refresh-1", "expiresIn": "120"}""")
        val auth = auth()
        auth.signIn {}

        assertEquals("id-2", auth.idToken())
        assertEquals("id-2", auth.idToken())

        assertEquals(1, count("/token"))
        assertEquals("refresh-2", store().load()!!.refreshToken)
    }

    @Test
    fun `a token with time left is not refreshed`() = runBlocking {
        val auth = auth()
        auth.signIn {}

        assertEquals("id-1", auth.idToken())

        assertEquals(0, count("/token"))
    }

    @Test
    fun `a token is refreshed when the clock has moved on`() = runBlocking {
        val clock = MutableClock()
        val auth = auth(clock)
        auth.signIn {}

        clock.advanceSeconds(3_600 - 4 * 60)

        assertEquals("id-2", auth.idToken())
    }

    @Test
    fun `a refresh Firebase refuses signs the user out`() = runBlocking {
        signInAnswer = ok("""{"localId": "uid-1", "idToken": "id-1", "refreshToken": "refresh-1", "expiresIn": "60"}""")
        refreshAnswer = failure("TOKEN_EXPIRED")
        val auth = auth()
        auth.signIn {}

        try {
            auth.idToken()
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unauthenticated) {
            assertNull(auth.currentUser.first())
            assertNull(store().load())
        }
    }

    @Test
    fun `a refresh that only failed to reach Firebase keeps the session`() = runBlocking {
        signInAnswer = ok("""{"localId": "uid-1", "idToken": "id-1", "refreshToken": "refresh-1", "expiresIn": "60"}""")
        refreshAnswer = HttpStatusCode.InternalServerError to "oops"
        val auth = auth()
        auth.signIn {}

        try {
            auth.idToken()
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unknown) {
            assertNotNull(auth.currentUser.first())
            assertNotNull(store().load())
        }
    }

    @Test
    fun `deleting without signing in again is refused`() = runBlocking {
        val auth = auth()
        auth.signIn {}

        try {
            auth.deleteUser()
            throw AssertionError("expected a failure")
        } catch (_: CloudException.RecentLoginRequired) {
            assertEquals(0, count("delete"))
            assertNotNull(store().load())
        }
    }

    @Test
    fun `deleting after signing in again removes the account and the session`() = runBlocking {
        val auth = auth()
        auth.signIn {}
        auth.reauthenticate {}

        auth.deleteUser()

        assertEquals(1, count("delete"))
        assertNull(auth.currentUser.first())
        assertNull(store().load())
    }

    @Test
    fun `signing in again as another account is not a re-authentication`() = runBlocking {
        val auth = auth()
        auth.signIn {}
        signInAnswer = ok("""{"localId": "uid-2", "idToken": "id-x", "refreshToken": "refresh-x", "expiresIn": "3600"}""")

        try {
            auth.reauthenticate {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.CredentialCollision) {
            assertEquals("uid-1", store().load()!!.uid)
        }
    }

    @Test
    fun `re-authenticating without a user is refused`() = runBlocking {
        try {
            auth().reauthenticate {}
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unauthenticated) {
            assertEquals(0, googleSignIns)
        }
    }

    @Test
    fun `signing out removes the session`() = runBlocking {
        val auth = auth()
        auth.signIn {}

        auth.signOut()

        assertNull(auth.currentUser.first())
        assertTrue(Files.notExists(folder.root.toPath().resolve("cloud-session.json")))
    }

    @Test
    fun `an id token without a session is unauthenticated`() = runBlocking {
        try {
            auth().idToken()
            throw AssertionError("expected a failure")
        } catch (_: CloudException.Unauthenticated) {
            assertEquals(0, count("/token"))
        }
    }
}
