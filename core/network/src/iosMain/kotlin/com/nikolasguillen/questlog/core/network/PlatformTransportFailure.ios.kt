package com.nikolasguillen.questlog.core.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLErrorCannotConnectToHost
import platform.Foundation.NSURLErrorCannotFindHost
import platform.Foundation.NSURLErrorDNSLookupFailed
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

@OptIn(ExperimentalForeignApi::class)
internal actual fun Throwable.toPlatformTransportFailure(): Throwable? {
    val error = (this as? DarwinHttpRequestException)?.origin ?: return null
    if (error.domain != NSURLErrorDomain) return null
    return when (error.code) {
        NSURLErrorTimedOut -> IgdbTimeoutException(this)
        NSURLErrorNotConnectedToInternet,
        NSURLErrorCannotFindHost,
        NSURLErrorCannotConnectToHost,
        NSURLErrorNetworkConnectionLost,
        NSURLErrorDNSLookupFailed -> IgdbConnectivityException(this)
        else -> null
    }
}
