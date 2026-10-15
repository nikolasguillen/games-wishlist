package com.nikolasguillen.questlog.core.cloud.error

/** The user closed the provider's sign-in UI, or the app left the foreground mid-flow. */
class CloudSignInCancelledException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
