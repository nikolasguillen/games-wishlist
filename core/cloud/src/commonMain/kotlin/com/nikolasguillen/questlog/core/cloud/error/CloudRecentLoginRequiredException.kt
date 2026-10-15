package com.nikolasguillen.questlog.core.cloud.error

/** The operation needs a fresh sign-in first. */
class CloudRecentLoginRequiredException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
