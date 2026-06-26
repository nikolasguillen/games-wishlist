package com.example.gameswishlist.core.ui.mapper

import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText

fun RepositoryError.toUiText(): UiText {
    return when (this) {
        RepositoryError.NoNetwork -> UiText.StringResource(R.string.error_no_network)
        RepositoryError.RequestTimeout -> UiText.StringResource(R.string.error_request_timeout)
        is RepositoryError.Http -> UiText.StringResource(R.string.error_http, code)
        is RepositoryError.Unknown -> UiText.StringResource(R.string.error_unknown)
    }
}
