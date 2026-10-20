package com.nikolasguillen.questlog.core.model

/**
 * Why a repository operation failed, as the rest of the app is allowed to see it.
 *
 * These are the only failure shapes that cross the `:core:data` boundary: exceptions are mapped here by
 * `Throwable.toRepositoryError()`, so no layer above has to know about HTTP clients or SQLite. The UI
 * layer turns each case into a `UiText`.
 */
sealed interface RepositoryError {
    /** The device could not reach the host at all — no connectivity, or DNS and connect failures. */
    data object NoNetwork : RepositoryError

    /** The host was reachable but did not answer in time. */
    data object RequestTimeout : RepositoryError

    /**
     * IGDB answered with a non-2xx status.
     *
     * @property code the HTTP status code.
     * @property message the status line, for logs — not something to show the user.
     */
    data class Http(val code: Int, val message: String? = null) : RepositoryError

    /** Reading or writing a file on the device failed, e.g. a wishlist cover image. */
    data object FileStorage : RepositoryError

    /** There is no signed-in account, or the session was revoked or expired during an operation. */
    data object NotSignedIn : RepositoryError

    /** The user closed a provider's sign-in UI. The screen shows nothing for it. */
    data object SignInCancelled : RepositoryError

    /** The device has no account to sign in with, so the provider's sign-in had nothing to offer. */
    data object NoAccountAvailable : RepositoryError

    /** Signing in collided with an account that belongs to a different sign-in method. */
    data object AccountExistsWithOtherMethod : RepositoryError

    /** The sign-in method being linked already belongs to another account. */
    data object MethodLinkedToOtherAccount : RepositoryError

    /** A restore was requested but the account has no backup. */
    data object BackupNotFound : RepositoryError

    /** The backup was written by a newer version of the app than the one reading it. */
    data object BackupFromNewerVersion : RepositoryError

    /** A part of the backup is missing or short, or its content cannot be decoded. Nothing was changed. */
    data object BackupUnreadable : RepositoryError

    /** The collection would need more parts than a backup may have. */
    data object BackupTooLarge : RepositoryError

    /**
     * Anything that did not match a case above.
     *
     * @property cause the original failure, kept for logging and debugging.
     */
    data class Unknown(val cause: Throwable? = null) : RepositoryError
}
