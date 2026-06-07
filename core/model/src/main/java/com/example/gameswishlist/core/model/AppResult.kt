package com.example.gameswishlist.core.model

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Failure(val error: RepositoryError) : AppResult<Nothing>()

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (RepositoryError) -> Unit): AppResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun failure(error: RepositoryError): AppResult<Nothing> = Failure(error)
    }
}