package com.example.gameswishlist.core.network

import java.io.IOException

/**
 * Raised when IGDB answers a request with a non-2xx status.
 *
 * This is the module's own type on purpose: it lets `:core:data` recognise an HTTP failure with a plain
 * `is` check instead of depending on Retrofit, and it survives a swap of the HTTP stack — only the code
 * that throws it would change.
 *
 * It extends [IOException] because it is thrown from an OkHttp interceptor, and that is the only kind of
 * failure the interceptor chain is allowed to surface.
 *
 * @property code the HTTP status code of the response.
 */
class IgdbHttpException(
    val code: Int,
    override val message: String
) : IOException(message)
