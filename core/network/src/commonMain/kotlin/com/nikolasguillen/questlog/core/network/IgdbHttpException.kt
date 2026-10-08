package com.nikolasguillen.questlog.core.network

import kotlinx.io.IOException

/**
 * Raised when IGDB answers a request with a non-2xx status.
 *
 * This is the module's own type on purpose: it lets `:core:data` recognise an HTTP failure with a plain
 * `is` check instead of depending on the HTTP client, and it survives a swap of the HTTP stack — only the
 * code that throws it would change.
 *
 * It extends [IOException] like the other transport failures this module reports ([IgdbTimeoutException],
 * [IgdbConnectivityException]).
 *
 * @property code the HTTP status code of the response.
 */
class IgdbHttpException(
    val code: Int,
    override val message: String
) : IOException(message)
