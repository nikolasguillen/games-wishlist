package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbConnectivityException
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import com.nikolasguillen.questlog.core.network.IgdbTimeoutException
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun Throwable.toRepositoryError(): RepositoryError {
    if (this is CancellationException) throw this

    return when {
        isConnectivityFailure() -> RepositoryError.NoNetwork

        isTimeoutFailure() -> RepositoryError.RequestTimeout

        this is IgdbHttpException -> RepositoryError.Http(code = code, message = message)

        else -> RepositoryError.Unknown(this)
    }
}

// The two checks below are the only places that name platform exception types, so they are the part that
// differs per platform once this module is shared. [SocketTimeoutException] does not extend
// [SocketException], so a timeout is its own branch and never swallowed by the connectivity one.

internal fun Throwable.isConnectivityFailure(): Boolean =
    this is UnknownHostException ||
        this is ConnectException ||
        this is SocketException ||
        this is IgdbConnectivityException

internal fun Throwable.isTimeoutFailure(): Boolean =
    this is SocketTimeoutException || this is IgdbTimeoutException
