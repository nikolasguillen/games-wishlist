package com.nikolasguillen.questlog.core.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSError
import platform.Foundation.NSURLErrorBadURL
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlatformTransportFailureTest {

    private fun failure(code: Long, domain: String = NSURLErrorDomain!!) =
        DarwinHttpRequestException(NSError.errorWithDomain(domain, code, null))

    @Test
    fun `a timeout becomes a timeout exception`() {
        assertIs<IgdbTimeoutException>(failure(NSURLErrorTimedOut).toPlatformTransportFailure())
    }

    @Test
    fun `no connection becomes a connectivity exception`() {
        assertIs<IgdbConnectivityException>(failure(NSURLErrorNotConnectedToInternet).toPlatformTransportFailure())
        assertIs<IgdbConnectivityException>(failure(NSURLErrorCannotConnectToHost).toPlatformTransportFailure())
    }

    @Test
    fun `an unrelated url error is left alone`() {
        assertNull(failure(NSURLErrorBadURL).toPlatformTransportFailure())
    }

    @Test
    fun `an error from another domain is left alone`() {
        assertNull(failure(NSURLErrorTimedOut, domain = "SomeOtherDomain").toPlatformTransportFailure())
    }

    @Test
    fun `a failure that is not a darwin request exception is left alone`() {
        assertNull(IllegalStateException("boom").toPlatformTransportFailure())
    }
}
