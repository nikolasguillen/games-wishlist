package com.nikolasguillen.questlog.core.cloud.error

/** There is no signed-in user, or the session was revoked, expired or refused by the rules. */
class CloudUnauthenticatedException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
