package com.nikolasguillen.questlog.core.cloud

/** What `accounts:signInWithIdp` returns for a successful sign-in. */
internal class IdpSignInResult(
    val uid: String,
    val email: String?,
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)
