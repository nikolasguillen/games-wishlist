package com.nikolasguillen.questlog.core.cloud

/** Hands out the signed-in user's Firebase ID token, refreshed when it is about to expire. */
internal fun interface IdTokenSupplier {

    /** Throws `CloudException.Unauthenticated` when nobody is signed in or the session was refused. */
    suspend fun idToken(): String
}
