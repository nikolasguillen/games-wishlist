package com.nikolasguillen.questlog.core.cloud.error

/** The credential being linked already belongs to another account, or the provider is already linked. */
class CloudCredentialAlreadyLinkedException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
