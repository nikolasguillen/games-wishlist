package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudConnectivityException
import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialAlreadyLinkedException
import com.nikolasguillen.questlog.core.cloud.error.CloudCredentialCollisionException
import com.nikolasguillen.questlog.core.cloud.error.CloudNoAccountException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudTimeoutException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
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
        this is CloudConnectivityException -> RepositoryError.NoNetwork

        this is CloudTimeoutException -> RepositoryError.RequestTimeout

        this is CloudUnauthenticatedException -> RepositoryError.NotSignedIn

        this is CloudSignInCancelledException -> RepositoryError.SignInCancelled

        this is CloudNoAccountException -> RepositoryError.NoAccountAvailable

        this is CloudCredentialCollisionException -> RepositoryError.AccountExistsWithOtherMethod

        this is CloudCredentialAlreadyLinkedException -> RepositoryError.MethodLinkedToOtherAccount

        // A backup stream that cannot be put back together: a part missing, a length that is off, JSON that is not ours.
        this is BackupUnreadableException -> RepositoryError.BackupUnreadable

        else -> RepositoryError.Unknown(this)
    }
}

// The platform's own "device is offline" and "request timed out" exception types are the part that differs per
// target, so the two checks are `expect`. Each `actual` also recognises this module's own exceptions from
// `:core:network` (`IgdbConnectivityException`, `IgdbTimeoutException`).

internal expect fun Throwable.isConnectivityFailure(): Boolean

internal expect fun Throwable.isTimeoutFailure(): Boolean
