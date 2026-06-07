package com.example.gameswishlist.core.model

sealed interface RepositoryError {
    data object NoNetwork : RepositoryError
    data object RequestTimeout : RepositoryError
    data class Http(val code: Int, val message: String? = null) : RepositoryError
    data class Unknown(val cause: Throwable? = null) : RepositoryError
}

