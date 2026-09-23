package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.R
import com.nikolasguillen.questlog.core.ui.model.UiText

fun RepositoryError.toUiText(): UiText {
    return when (this) {
        RepositoryError.NoNetwork -> UiText.StringResource(R.string.error_no_network)
        RepositoryError.RequestTimeout -> UiText.StringResource(R.string.error_request_timeout)
        is RepositoryError.Http -> UiText.StringResource(R.string.error_http, code)
        RepositoryError.FileStorage -> UiText.StringResource(R.string.error_file_storage)
        is RepositoryError.Unknown -> UiText.StringResource(R.string.error_unknown)
    }
}
