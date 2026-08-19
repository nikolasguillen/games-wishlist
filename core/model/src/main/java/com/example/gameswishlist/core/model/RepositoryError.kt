package com.example.gameswishlist.core.model

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

    /**
     * Anything that did not match a case above.
     *
     * @property cause the original failure, kept for logging and debugging.
     */
    data class Unknown(val cause: Throwable? = null) : RepositoryError
}
