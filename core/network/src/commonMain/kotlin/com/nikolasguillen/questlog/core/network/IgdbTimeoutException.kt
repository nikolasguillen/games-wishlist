package com.nikolasguillen.questlog.core.network

import kotlinx.io.IOException

/**
 * Raised when an IGDB request does not complete in time, whichever HTTP engine was waiting.
 *
 * Like [IgdbHttpException] it is this module's own type, so `:core:data` can recognise a timeout with a
 * plain `is` check without depending on the HTTP client's timeout classes.
 */
class IgdbTimeoutException(cause: Throwable? = null) : IOException("IGDB request timed out", cause)
