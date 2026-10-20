package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.RepositoryError
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.error_http
import com.nikolasguillen.questlog.core.ui.resources.error_service_unavailable
import com.nikolasguillen.questlog.core.ui.resources.error_unknown
import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers how [toUiText] words an HTTP failure: by what the user can do about it, not by the raw status. */
class ErrorMapperTest {

    @Test
    fun `a rejected credential shows the generic message, never the status code`() {
        listOf(401, 403).forEach { code ->
            assertEquals(UiText.StringResource(Res.string.error_unknown), RepositoryError.Http(code).toUiText())
        }
    }

    @Test
    fun `a rate limit or a server failure asks the user to try later`() {
        listOf(429, 500, 503, 599).forEach { code ->
            assertEquals(
                UiText.StringResource(Res.string.error_service_unavailable),
                RepositoryError.Http(code).toUiText()
            )
        }
    }

    @Test
    fun `any other status keeps the code in the message`() {
        listOf(400, 404, 600).forEach { code ->
            assertEquals(UiText.StringResource(Res.string.error_http, code), RepositoryError.Http(code).toUiText())
        }
    }
}
