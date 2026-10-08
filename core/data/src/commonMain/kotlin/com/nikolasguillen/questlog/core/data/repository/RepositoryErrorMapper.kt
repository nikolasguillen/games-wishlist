package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import kotlinx.coroutines.CancellationException

internal fun Throwable.toRepositoryError(): RepositoryError {
    if (this is CancellationException) throw this

    return when {
        isConnectivityFailure() -> RepositoryError.NoNetwork

        isTimeoutFailure() -> RepositoryError.RequestTimeout

        this is IgdbHttpException -> RepositoryError.Http(code = code, message = message)

        else -> RepositoryError.Unknown(this)
    }
}

// The platform's own "device is offline" and "request timed out" exception types are the part that differs per
// target, so the two checks are `expect`. Each `actual` also recognises this module's own exceptions from
// `:core:network` (`IgdbConnectivityException`, `IgdbTimeoutException`).

internal expect fun Throwable.isConnectivityFailure(): Boolean

internal expect fun Throwable.isTimeoutFailure(): Boolean
