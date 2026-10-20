package com.nikolasguillen.questlog.core.cloud.error

/**
 * The only failure type `:core:cloud` throws, besides `CancellationException`. It extends [Exception] and not an
 * IO exception, so the error mapper's ordering rule about IO exceptions is unaffected.
 *
 * It is sealed so that the mapper in `:core:data` can switch over it exhaustively: a new failure added here does not
 * compile there until it has been given a `RepositoryError`.
 */
sealed class CloudException(message: String?, cause: Throwable?) : Exception(message, cause) {

    /** The device could not reach Firebase: it is offline, or the network failed. */
    class Connectivity(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** A Firebase call did not answer within the time allowed. */
    class Timeout(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** There is no signed-in user, or the session was revoked, expired or refused by the rules. */
    class Unauthenticated(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** The operation needs a fresh sign-in first. */
    class RecentLoginRequired(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** The user closed the provider's sign-in UI, or the app left the foreground mid-flow. */
    class SignInCancelled(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** The device has no account the provider could sign in with, so there was nothing to pick from. */
    class NoAccount(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** The sign-in collided with an account that belongs to a different sign-in method. */
    class CredentialCollision(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** The credential being linked already belongs to another account, or the provider is already linked. */
    class CredentialAlreadyLinked(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)

    /** A failure with nothing the user can act on or the app can tell apart, such as a provider returning no token. */
    class Unknown(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
}
