package com.nikolasguillen.questlog.core.cloud

/** What the secure-token service returns for a refresh. The refresh token may have changed. */
internal class TokenRefreshResult(
    val uid: String,
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long
)
