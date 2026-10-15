package com.nikolasguillen.questlog.core.cloud

/**
 * The provider sign-in that only Swift can run (GoogleSignIn-iOS and AuthenticationServices), implemented under
 * `iosApp/` and bound by `startQuestLog`.
 *
 * Nothing here is `suspend`: a `suspend` member implemented in Swift has no cancellation and crashes the process on a
 * thrown Swift error, so Kotlin wraps each call in a cancellable coroutine instead. Every `onResult` is called
 * exactly once, on any thread. Swift owns presentation, the Apple nonce and the Google client id.
 */
interface IdentityProviderBridge {
    fun signInWithGoogle(onResult: (GoogleSignInResult) -> Unit)

    fun signInWithApple(onResult: (AppleSignInResult) -> Unit)

    /** Revokes the token behind [authorizationCode] at Apple. `onResult` receives whether it worked. */
    fun revokeAppleToken(authorizationCode: String, onResult: (Boolean) -> Unit)

    fun signOutGoogle()
}
