package com.nikolasguillen.questlog.core.cloud

/**
 * The outcome of Sign in with Apple.
 *
 * @property idToken the identity token, set when [outcome] is [ProviderOutcome.SUCCESS].
 * @property rawNonce the unhashed nonce whose SHA-256 was sent to Apple, set on success.
 * @property authorizationCode kept to revoke the token when the account is deleted, set on success.
 */
class AppleSignInResult(
    val outcome: ProviderOutcome,
    val idToken: String?,
    val rawNonce: String?,
    val authorizationCode: String?
)
