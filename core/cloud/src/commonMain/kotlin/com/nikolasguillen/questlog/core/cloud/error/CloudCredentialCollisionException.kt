package com.nikolasguillen.questlog.core.cloud.error

/** The sign-in collided with an account that belongs to a different sign-in method. */
class CloudCredentialCollisionException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
