package com.nikolasguillen.questlog.core.cloud

/**
 * Sign in with Apple, which only Swift can run (AuthenticationServices), implemented under `iosApp/` and bound by
 * `startQuestLog`. Apple is the only method iOS offers.
 *
 * Nothing here is `suspend`: a `suspend` member implemented in Swift has no cancellation and crashes the process on a
 * thrown Swift error, so Kotlin wraps each call in a cancellable coroutine instead. Every `onResult` is called
 * exactly once, on any thread. Swift owns presentation and the nonce.
 */
interface IdentityProviderBridge {
    fun signInWithApple(onResult: (AppleSignInResult) -> Unit)

    /** Revokes the token behind [authorizationCode] at Apple. `onResult` receives whether it worked. */
    fun revokeAppleToken(authorizationCode: String, onResult: (Boolean) -> Unit)
}
