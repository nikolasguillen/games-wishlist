package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.network.IgdbConnectivityException
import com.nikolasguillen.questlog.core.network.IgdbTimeoutException

// The Darwin engine's NSError-based failures are already translated into these two types by `:core:network`
// (`PlatformTransportFailure.ios.kt`), so there is no platform exception to name here.

internal actual fun Throwable.isConnectivityFailure(): Boolean = this is IgdbConnectivityException

internal actual fun Throwable.isTimeoutFailure(): Boolean = this is IgdbTimeoutException
