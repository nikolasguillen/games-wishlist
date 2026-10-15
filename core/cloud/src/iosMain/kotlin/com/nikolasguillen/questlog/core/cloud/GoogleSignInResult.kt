package com.nikolasguillen.questlog.core.cloud

/**
 * The outcome of Google's sign-in.
 *
 * @property idToken set when [outcome] is [ProviderOutcome.SUCCESS].
 * @property accessToken set when [outcome] is [ProviderOutcome.SUCCESS].
 */
class GoogleSignInResult(
    val outcome: ProviderOutcome,
    val idToken: String?,
    val accessToken: String?
)
