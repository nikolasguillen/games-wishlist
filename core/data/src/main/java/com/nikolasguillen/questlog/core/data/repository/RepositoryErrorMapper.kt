package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal fun Throwable.toRepositoryError(): RepositoryError {
    if (this is CancellationException) throw this

    return when (this) {
        is UnknownHostException,
        is ConnectException,
        is SocketException -> RepositoryError.NoNetwork

        is SocketTimeoutException -> RepositoryError.RequestTimeout

        is IgdbHttpException -> RepositoryError.Http(code = code, message = message)

        else -> RepositoryError.Unknown(this)
    }
}
