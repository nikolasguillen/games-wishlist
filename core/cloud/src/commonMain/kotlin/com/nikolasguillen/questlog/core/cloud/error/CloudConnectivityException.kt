package com.nikolasguillen.questlog.core.cloud.error

/** The device could not reach Firebase: it is offline, or the network failed. */
class CloudConnectivityException(message: String? = null, cause: Throwable? = null) : CloudException(message, cause)
