package com.example.gameswishlist.core.model

/**
 * The outcome of an operation that can fail with a typed error instead of an exception.
 *
 * Returned by the repository methods that touch the network — `:core:data` is where exceptions stop and
 * become a [Failure]. Database-only operations return a bare `Flow` or `Unit` and never this type.
 *
 * Build instances through [success] and [failure] rather than the subclass constructors.
 *
 * @param T the value carried by a successful outcome.
 */
sealed class AppResult<out T> {
    /**
     * A completed operation.
     *
     * @property data the value produced by the operation.
     */
    data class Success<out T>(val data: T) : AppResult<T>()

    /**
     * A failed operation. Carries no value, hence `AppResult<Nothing>`.
     *
     * @property error what went wrong, already mapped to a domain error by `:core:data`.
     */
    data class Failure(val error: RepositoryError) : AppResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (RepositoryError) -> Unit): AppResult<T> {
        if (this is Failure) action(error)
        return this
    }

    /**
     * Applies [transform] to the value of a [Success], leaving a [Failure] untouched. Use this to carry a
     * result across layers instead of unwrapping and rewrapping it.
     */
    inline fun <R> map(transform: (T) -> R): AppResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> Failure(error)
        }
    }

    /**
     * Combines this result with another. If both have success, applies [transform].
     * If one of them fails, returns the first encountered error.
     */
    inline fun <R, V> zip(other: AppResult<R>, transform: (T, R) -> V): AppResult<V> {
        return when (this) {
            is Success -> when (other) {
                is Success -> Success(transform(data, other.data))
                is Failure -> other
            }
            is Failure -> this
        }
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun failure(error: RepositoryError): AppResult<Nothing> = Failure(error)
    }
}
