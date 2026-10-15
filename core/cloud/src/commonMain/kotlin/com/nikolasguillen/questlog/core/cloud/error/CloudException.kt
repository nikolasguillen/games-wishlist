package com.nikolasguillen.questlog.core.cloud.error

/**
 * The only failure type `:core:cloud` throws, besides `CancellationException`. It extends [Exception] and not an
 * IO exception, so the error mapper's ordering rule about IO exceptions is unaffected.
 */
open class CloudException(message: String?, cause: Throwable? = null) : Exception(message, cause)
