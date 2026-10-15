package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.error.CloudSignInCancelledException
import com.nikolasguillen.questlog.core.cloud.error.CloudUnauthenticatedException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val APPLE_PROVIDER_ID = "apple.com"

/** The provider sign-in over the Swift [IdentityProviderBridge]; the tokens it returns go to GitLive. */
internal class IosProviderAuth(private val bridge: IdentityProviderBridge) : PlatformProviderAuth {

    private val auth get() = Firebase.auth

    override suspend fun signIn(provider: CloudProvider) {
        when (provider) {
            CloudProvider.GOOGLE -> auth.signInWithCredential(googleCredential())
            CloudProvider.APPLE -> auth.signInWithCredential(appleSignIn().credential)
        }
    }

    override suspend fun link(provider: CloudProvider) {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        when (provider) {
            CloudProvider.GOOGLE -> user.linkWithCredential(googleCredential())
            CloudProvider.APPLE -> user.linkWithCredential(appleSignIn().credential)
        }
    }

    override suspend fun reauthenticate(provider: CloudProvider): AppleGrant? {
        val user = auth.currentUser ?: throw CloudUnauthenticatedException("Nobody is signed in")
        return when (provider) {
            CloudProvider.GOOGLE -> {
                user.reauthenticate(googleCredential())
                null
            }

            CloudProvider.APPLE -> {
                val apple = appleSignIn()
                user.reauthenticate(apple.credential)
                AppleGrant(apple.authorizationCode)
            }
        }
    }

    override suspend fun revokeApple(grant: AppleGrant) {
        val revoked = suspendCancellableCoroutine { continuation ->
            bridge.revokeAppleToken(grant.value) { if (continuation.isActive) continuation.resume(it) }
        }
        if (!revoked) throw CloudException("Apple did not revoke the token")
    }

    override suspend fun clearProviderState() = bridge.signOutGoogle()

    private suspend fun googleCredential(): AuthCredential {
        val result = suspendCancellableCoroutine { continuation ->
            bridge.signInWithGoogle { if (continuation.isActive) continuation.resume(it) }
        }
        result.outcome.throwUnlessSuccess("Google")
        val idToken = result.idToken ?: throw CloudException("Google returned no ID token")
        return GoogleAuthProvider.credential(idToken, result.accessToken)
    }

    private suspend fun appleSignIn(): AppleSignIn {
        val result = suspendCancellableCoroutine { continuation ->
            bridge.signInWithApple { if (continuation.isActive) continuation.resume(it) }
        }
        result.outcome.throwUnlessSuccess("Apple")
        val idToken = result.idToken ?: throw CloudException("Apple returned no identity token")
        val rawNonce = result.rawNonce ?: throw CloudException("Apple returned no nonce")
        val authorizationCode = result.authorizationCode ?: throw CloudException("Apple returned no authorization code")
        return AppleSignIn(
            credential = OAuthProvider.credential(providerId = APPLE_PROVIDER_ID, idToken = idToken, rawNonce = rawNonce),
            authorizationCode = authorizationCode
        )
    }

    private fun ProviderOutcome.throwUnlessSuccess(provider: String) {
        when (this) {
            ProviderOutcome.SUCCESS -> Unit
            ProviderOutcome.CANCELLED -> throw CloudSignInCancelledException("The $provider sign-in was closed")
            ProviderOutcome.FAILED -> throw CloudException("The $provider sign-in failed")
        }
    }

    private class AppleSignIn(val credential: AuthCredential, val authorizationCode: String)
}
