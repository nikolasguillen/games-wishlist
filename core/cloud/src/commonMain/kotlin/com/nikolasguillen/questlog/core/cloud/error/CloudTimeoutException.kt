package com.nikolasguillen.questlog.core.cloud.error

/** A Firebase call did not answer within the time allowed. */
class CloudTimeoutException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
