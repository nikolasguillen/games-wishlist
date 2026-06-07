package com.example.gameswishlist.core.data.repository

import com.example.gameswishlist.core.model.RepositoryError
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

        else -> toHttpRepositoryErrorOrNull() ?: RepositoryError.Unknown(this)
    }
}

private fun Throwable.toHttpRepositoryErrorOrNull(): RepositoryError.Http? {
    if (javaClass.name != "retrofit2.HttpException") return null

    val code = runCatching {
        javaClass.getMethod("code").invoke(this) as? Int
    }.getOrNull()

    return RepositoryError.Http(
        code = code ?: -1,
        message = message
    )
}


