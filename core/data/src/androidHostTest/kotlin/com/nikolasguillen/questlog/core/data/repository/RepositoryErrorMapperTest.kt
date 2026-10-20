package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudConnectivityException
import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialAlreadyLinkedException
import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException
import com.nikolasguillen.questlog.core.cloud.error.CloudNoAccountException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudTimeoutException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.data.backup.BackupUnreadableException
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbConnectivityException
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import com.nikolasguillen.questlog.core.network.IgdbTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class RepositoryErrorMapperTest {

    @Test
    fun `unknown host maps to no network`() {
        assertEquals(RepositoryError.NoNetwork, UnknownHostException().toRepositoryError())
    }

    @Test
    fun `refused connection maps to no network`() {
        assertEquals(RepositoryError.NoNetwork, ConnectException().toRepositoryError())
    }

    @Test
    fun `socket failure maps to no network`() {
        assertEquals(RepositoryError.NoNetwork, SocketException().toRepositoryError())
    }

    /**
     * [SocketTimeoutException] doesn't extend [SocketException], so it has to keep landing on its
     * own branch rather than being swallowed by the no-network one.
     */
    @Test
    fun `socket timeout maps to request timeout`() {
        assertEquals(RepositoryError.RequestTimeout, SocketTimeoutException().toRepositoryError())
    }

    /**
     * [IgdbHttpException] extends [IOException], so it has to be matched before the catch-all branch
     * that would otherwise report every HTTP failure as [RepositoryError.Unknown].
     */
    @Test
    fun `http exception maps to http error carrying the status code`() {
        val exception = IgdbHttpException(code = 404, message = "HTTP 404 Not Found")

        val error = exception.toRepositoryError()

        assertEquals(404, (error as RepositoryError.Http).code)
        assertEquals("HTTP 404 Not Found", error.message)
    }

    /** The HTTP client's own timeouts reach this layer as the network module's type, never as the client's. */
    @Test
    fun `an IGDB timeout maps to request timeout`() {
        assertEquals(RepositoryError.RequestTimeout, IgdbTimeoutException().toRepositoryError())
    }

    @Test
    fun `an IGDB connectivity failure maps to no network`() {
        assertEquals(RepositoryError.NoNetwork, IgdbConnectivityException().toRepositoryError())
    }

    /**
     * Both of the network module's transport exceptions extend [IOException], like [IgdbHttpException], so
     * they must be matched ahead of the catch-all.
     */
    @Test
    fun `the network module's transport exceptions are not reported as unknown`() {
        assertEquals(RepositoryError.RequestTimeout, IgdbTimeoutException(IOException("slow")).toRepositoryError())
        assertEquals(RepositoryError.NoNetwork, IgdbConnectivityException(IOException("down")).toRepositoryError())
    }

    @Test
    fun `a cloud connectivity failure maps to no network`() {
        assertEquals(RepositoryError.NoNetwork, CloudConnectivityException().toRepositoryError())
    }

    @Test
    fun `a cloud timeout maps to request timeout`() {
        assertEquals(RepositoryError.RequestTimeout, CloudTimeoutException().toRepositoryError())
    }

    @Test
    fun `a cloud unauthenticated failure maps to not signed in`() {
        assertEquals(RepositoryError.NotSignedIn, CloudUnauthenticatedException().toRepositoryError())
    }

    @Test
    fun `a cancelled cloud sign-in maps to sign-in cancelled`() {
        assertEquals(RepositoryError.SignInCancelled, CloudSignInCancelledException().toRepositoryError())
    }

    @Test
    fun `a missing account maps to no account available and not to a cancelled sign-in`() {
        assertEquals(RepositoryError.NoAccountAvailable, CloudNoAccountException().toRepositoryError())
    }

    @Test
    fun `a credential collision maps to account exists with other method`() {
        assertEquals(
            RepositoryError.AccountExistsWithOtherMethod,
            CloudCredentialCollisionException().toRepositoryError()
        )
    }

    @Test
    fun `an already linked credential maps to method linked to other account`() {
        assertEquals(
            RepositoryError.MethodLinkedToOtherAccount,
            CloudCredentialAlreadyLinkedException().toRepositoryError()
        )
    }

    @Test
    fun `an unreadable backup maps to backup unreadable`() {
        assertEquals(RepositoryError.BackupUnreadable, BackupUnreadableException("short").toRepositoryError())
    }

    @Test
    fun `unrecognised failure maps to unknown keeping the cause`() {
        val exception = IOException("disk full")

        val error = exception.toRepositoryError()

        assertSame(exception, (error as RepositoryError.Unknown).cause)
    }

    /**
     * Cancellation is how structured concurrency unwinds a coroutine, so it must propagate
     * instead of being reported to the user as a failure.
     */
    @Test
    fun `cancellation is rethrown rather than mapped`() {
        val exception = CancellationException("scope closed")

        val thrown = assertThrows(CancellationException::class.java) {
            exception.toRepositoryError()
        }

        assertSame(exception, thrown)
    }
}
