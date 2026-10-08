package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.error_file_storage
import com.nikolasguillen.questlog.core.ui.resources.error_http
import com.nikolasguillen.questlog.core.ui.resources.error_no_network
import com.nikolasguillen.questlog.core.ui.resources.error_request_timeout
import com.nikolasguillen.questlog.core.ui.resources.error_unknown

fun RepositoryError.toUiText(): UiText {
    return when (this) {
        RepositoryError.NoNetwork -> UiText.StringResource(Res.string.error_no_network)
        RepositoryError.RequestTimeout -> UiText.StringResource(Res.string.error_request_timeout)
        is RepositoryError.Http -> UiText.StringResource(Res.string.error_http, code)
        RepositoryError.FileStorage -> UiText.StringResource(Res.string.error_file_storage)
        is RepositoryError.Unknown -> UiText.StringResource(Res.string.error_unknown)
    }
}
