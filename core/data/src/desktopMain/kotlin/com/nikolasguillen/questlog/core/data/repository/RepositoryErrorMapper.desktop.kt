package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.network.IgdbConnectivityException
import com.nikolasguillen.questlog.core.network.IgdbTimeoutException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// [SocketTimeoutException] does not extend [SocketException], so a timeout is its own branch and never
// swallowed by the connectivity one.

internal actual fun Throwable.isConnectivityFailure(): Boolean =
    this is UnknownHostException ||
        this is ConnectException ||
        this is SocketException ||
        this is IgdbConnectivityException

internal actual fun Throwable.isTimeoutFailure(): Boolean =
    this is SocketTimeoutException || this is IgdbTimeoutException
