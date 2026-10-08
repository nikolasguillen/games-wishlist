package com.nikolasguillen.questlog.core.network

import java.io.IOException

/**
 * Raised when the device cannot reach IGDB at all: no connection, a failed lookup, a refused connect.
 *
 * Where the HTTP engine already reports these as the platform's own exception types, `:core:data` matches
 * those directly; this type is for an engine whose failures can only be told apart inside `:core:network`.
 */
class IgdbConnectivityException(cause: Throwable? = null) : IOException("IGDB is unreachable", cause)
