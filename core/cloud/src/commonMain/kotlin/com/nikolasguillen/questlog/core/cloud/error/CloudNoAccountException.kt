package com.nikolasguillen.questlog.core.cloud.error

/** The device has no account the provider could sign in with, so there was nothing to pick from. */
class CloudNoAccountException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
