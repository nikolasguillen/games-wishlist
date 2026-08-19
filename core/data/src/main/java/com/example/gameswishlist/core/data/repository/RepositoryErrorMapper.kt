package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.network.IgdbHttpException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException

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
