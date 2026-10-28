package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RestErrorMapperTest {

    @Test
    fun `identity toolkit codes map to their exceptions`() {
        assertTrue(RestErrorMapper.fromIdentityToolkitMessage("FEDERATED_USER_ID_ALREADY_LINKED") is CloudException.CredentialCollision)
        assertTrue(RestErrorMapper.fromIdentityToolkitMessage("CREDENTIAL_TOO_OLD_LOGIN_AGAIN") is CloudException.RecentLoginRequired)
        listOf("TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN", "INVALID_ID_TOKEN", "USER_DISABLED", "USER_NOT_FOUND").forEach {
            assertTrue(it, RestErrorMapper.fromIdentityToolkitMessage(it) is CloudException.Unauthenticated)
        }
        listOf("INVALID_IDP_RESPONSE", "INVALID_API_KEY", "SOMETHING_NEW", "", null).forEach {
            assertTrue(it, RestErrorMapper.fromIdentityToolkitMessage(it) is CloudException.Unknown)
        }
    }

    @Test
    fun `only the code before the colon counts`() {
        assertTrue(
            RestErrorMapper.fromIdentityToolkitMessage("TOKEN_EXPIRED : the user's credential is no longer valid") is
                CloudException.Unauthenticated
        )
    }

    @Test
    fun `an error body is read for its message`() {
        val body = """{"error": {"code": 400, "message": "INVALID_ID_TOKEN", "errors": []}}"""

        assertTrue(RestErrorMapper.fromIdentityToolkitBody(body) is CloudException.Unauthenticated)
        assertTrue(RestErrorMapper.fromIdentityToolkitBody("<html>bad gateway</html>") is CloudException.Unknown)
        assertTrue(RestErrorMapper.fromIdentityToolkitBody("{}") is CloudException.Unknown)
    }

    @Test
    fun `firestore statuses map to their exceptions`() {
        assertTrue(RestErrorMapper.fromFirestoreStatus(401) is CloudException.Unauthenticated)
        assertTrue(RestErrorMapper.fromFirestoreStatus(403) is CloudException.Unauthenticated)
        assertTrue(RestErrorMapper.fromFirestoreStatus(500) is CloudException.Unknown)
        assertTrue(RestErrorMapper.fromFirestoreStatus(429) is CloudException.Unknown)
    }

    @Test
    fun `transport failures are connectivity failures`() = runTest {
        listOf(UnknownHostException(), ConnectException(), SocketException()).forEach { failure ->
            try {
                RestErrorMapper.call<Unit> { throw failure }
                throw AssertionError("expected a failure")
            } catch (e: CloudException.Connectivity) {
                assertEquals(failure::class, e.cause!!::class)
            }
        }
    }

    @Test
    fun `a socket timeout is a timeout`() = runTest {
        try {
            RestErrorMapper.call<Unit> { throw SocketTimeoutException() }
            throw AssertionError("expected a failure")
        } catch (e: CloudException.Timeout) {
            // expected
        }
    }

    @Test
    fun `a call over the limit is a timeout`() = runTest {
        try {
            RestErrorMapper.call { delay(CLOUD_CALL_TIMEOUT * 2) }
            throw AssertionError("expected a failure")
        } catch (e: CloudException.Timeout) {
            // expected
        }
    }

    @Test
    fun `a cloud exception passes through as it is`() = runTest {
        val failure = CloudException.CredentialCollision("x")

        try {
            RestErrorMapper.call<Unit> { throw failure }
            throw AssertionError("expected a failure")
        } catch (e: CloudException.CredentialCollision) {
            // Coroutines may hand back a copy of the exception with a recovered stack trace, so compare what it says.
            assertEquals("x", e.message)
        }
    }

    @Test
    fun `cancellation is rethrown as it is`() = runTest {
        val cancellation = CancellationException("stop")

        try {
            RestErrorMapper.call<Unit> { throw cancellation }
            throw AssertionError("expected a failure")
        } catch (e: CancellationException) {
            assertEquals("stop", e.message)
            assertTrue(e !is kotlinx.coroutines.TimeoutCancellationException)
        }
    }

    @Test
    fun `anything else is an unknown failure`() = runTest {
        try {
            RestErrorMapper.call<Unit> { error("boom") }
            throw AssertionError("expected a failure")
        } catch (e: CloudException.Unknown) {
            assertEquals("boom", e.cause?.message)
        }
    }
}
