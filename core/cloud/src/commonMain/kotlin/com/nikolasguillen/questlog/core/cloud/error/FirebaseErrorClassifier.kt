package com.nikolasguillen.questlog.core.cloud.error

import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.code
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.code
import kotlin.coroutines.cancellation.CancellationException

/**
 * Turns a Firestore status into this module's failure, or `null` when the caller should rethrow the original.
 *
 * It takes the status *name* rather than the enum: on Android the enum is Firebase's own class, which cannot be
 * initialised in a JVM unit test.
 */
internal fun classifyFirestoreCode(codeName: String, cause: Throwable? = null): CloudException? = when (codeName) {
    "UNAVAILABLE" -> CloudConnectivityException("Firestore is unavailable: $codeName", cause)
    "DEADLINE_EXCEEDED" -> CloudTimeoutException("Firestore call timed out: $codeName", cause)
    "UNAUTHENTICATED", "PERMISSION_DENIED" -> CloudUnauthenticatedException("Firestore refused the call: $codeName", cause)
    else -> null
}

/**
 * Turns a Firebase Auth error code into this module's failure, or `null` when the caller should rethrow the original.
 *
 * The same condition has two spellings: Android reports the `ERROR_*` name and iOS reports the `AuthErrorCode` raw value
 * as a number in a string (GitLive passes it through unchanged), so both are listed.
 */
internal fun classifyAuthCode(code: String?, cause: Throwable? = null): CloudException? = when (code) {
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_EMAIL_ALREADY_IN_USE",
    "17012", "17007" -> CloudCredentialCollisionException("Auth collision: $code", cause)

    "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_PROVIDER_ALREADY_LINKED",
    "17025", "17015" -> CloudCredentialAlreadyLinkedException("Credential already linked: $code", cause)

    "ERROR_REQUIRES_RECENT_LOGIN", "17014" -> CloudRecentLoginRequiredException("Recent login required: $code", cause)

    "ERROR_NETWORK_REQUEST_FAILED", "17020" -> CloudConnectivityException("Auth network failure: $code", cause)

    "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN", "ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED",
    "17021", "17017", "17011", "17005" -> CloudUnauthenticatedException("Session is no longer valid: $code", cause)

    else -> null
}

/**
 * Maps a Firebase failure to a [CloudException], so that `:core:data` never has to name a Firebase type. A
 * `CancellationException` is rethrown first, and a failure nothing matches comes back unchanged.
 */
internal fun Throwable.toCloudException(): Throwable {
    if (this is CancellationException) throw this
    if (this is CloudException) return this
    val classified = when (this) {
        is FirebaseFirestoreException -> classifyFirestoreCode(code.name, this)
        is FirebaseNetworkException -> CloudConnectivityException("Network failure", this)
        is FirebaseAuthException -> classifyAuthCode(code, this)
        else -> null
    }
    return classified ?: this
}
