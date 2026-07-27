package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.model.RepositoryError
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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

    @Test
    fun `http exception maps to http error carrying the status code`() {
        val exception = HttpException(Response.error<Any>(404, "".toResponseBody()))

        val error = exception.toRepositoryError()

        assertEquals(404, (error as RepositoryError.Http).code)
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
