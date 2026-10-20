package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.data.backup.BackupUnreadableException
import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.network.IgdbHttpException
import kotlinx.coroutines.CancellationException

internal fun Throwable.toRepositoryError(): RepositoryError {
    if (this is CancellationException) throw this

    return when {
        isConnectivityFailure() -> RepositoryError.NoNetwork

        isTimeoutFailure() -> RepositoryError.RequestTimeout

        this is IgdbHttpException -> RepositoryError.Http(code = code, message = message)

        // `:core:cloud` already translated Firebase's own failures, so no Firebase type is named here.
        this is CloudException -> toCloudRepositoryError()

        // A backup stream that cannot be put back together: a part missing, a length that is off, JSON that is not ours.
        this is BackupUnreadableException -> RepositoryError.BackupUnreadable

        else -> RepositoryError.Unknown(this)
    }
}

// Exhaustive on purpose: a new `CloudException` does not compile until it has been given a `RepositoryError` here.
private fun CloudException.toCloudRepositoryError(): RepositoryError = when (this) {
    is CloudException.Connectivity -> RepositoryError.NoNetwork
    is CloudException.Timeout -> RepositoryError.RequestTimeout
    is CloudException.Unauthenticated -> RepositoryError.NotSignedIn
    is CloudException.SignInCancelled -> RepositoryError.SignInCancelled
    is CloudException.NoAccount -> RepositoryError.NoAccountAvailable
    is CloudException.CredentialCollision -> RepositoryError.AccountExistsWithOtherMethod
    is CloudException.CredentialAlreadyLinked -> RepositoryError.MethodLinkedToOtherAccount
    // `AccountRepositoryImpl.deleteAccount` answers this one itself, so it reaches the screen only as a surprise.
    is CloudException.RecentLoginRequired, is CloudException.Unknown -> RepositoryError.Unknown(this)
}

// The platform's own "device is offline" and "request timed out" exception types are the part that differs per
// target, so the two checks are `expect`. Each `actual` also recognises this module's own exceptions from
// `:core:network` (`IgdbConnectivityException`, `IgdbTimeoutException`).

internal expect fun Throwable.isConnectivityFailure(): Boolean

internal expect fun Throwable.isTimeoutFailure(): Boolean
