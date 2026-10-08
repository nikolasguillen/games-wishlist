package com.nikolasguillen.questlog.core.network

/**
 * Turns a failure that only this platform's HTTP engine can describe into this module's own
 * [IgdbConnectivityException] or [IgdbTimeoutException], or returns `null` when it is not one.
 *
 * The OkHttp engine on Android reports "device is offline" as `java.net` exceptions, which `:core:data` already
 * matches itself, so there is nothing to do there. The Darwin engine on iOS wraps an `NSError` in a Ktor type, and
 * `:core:data` must not depend on Ktor - so the translation is here.
 */
internal expect fun Throwable.toPlatformTransportFailure(): Throwable?
